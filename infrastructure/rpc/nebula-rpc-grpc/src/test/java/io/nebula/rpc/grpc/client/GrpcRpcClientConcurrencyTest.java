package io.nebula.rpc.grpc.client;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import io.nebula.rpc.grpc.test.TestRpcService;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * {@link GrpcRpcClient#callWithTarget} 无锁化的并发见证与不串台测试。
 * <p>
 * 生产故障背景: {@code ConfigurableRpcClient.callWithTarget} 默认实现
 * {@code synchronized(this){ setTargetAddress + call }} 在整个 RPC 往返期间持锁,
 * 使同一代理 bean 的全部调用串行化(有效并发被钉死为 1)。GrpcRpcClient 已重写为
 * 按目标地址缓存 Channel 的无锁版本。本测试从行为上证明:
 * <ol>
 *   <li>{@link #concurrentCallsThroughCallWithTargetDoNotSerialize} 无锁版并发不互斥(正向见证);</li>
 *   <li>{@link #synchronizedDefaultWouldSerializeAndTimeOut} 复刻 synchronized 默认版必然串行超时(负向注入),
 *       且以 {@link AssertionFailedError}(Failure 而非 Error) 形态呈现;</li>
 *   <li>{@link #concurrentCallsToDifferentTargetsDoNotCrossTalk} 并发打不同地址时请求落到正确实例(不串台)。</li>
 * </ol>
 * 服务端用真实 Netty(shaded) gRPC server, 客户端经 {@code NettyChannelBuilder.forTarget} 连接。
 */
class GrpcRpcClientConcurrencyTest {

    /**
     * 集合点服务: 每次调用先 countDown 再 await 同一个 latch。
     * 只有当全部并发调用都真正到达服务端(说明客户端没有串行化), latch 才归零、各调用才返回。
     * 若客户端把调用串行化, 后续调用永远发不出, 先到的调用会一直阻塞到 awaitMillis 超时。
     */
    private static final class RendezvousService extends GenericRpcServiceGrpc.GenericRpcServiceImplBase {
        private final String identity;
        private final CountDownLatch rendezvous;
        private final long awaitMillis;

        RendezvousService(String identity, CountDownLatch rendezvous, long awaitMillis) {
            this.identity = identity;
            this.rendezvous = rendezvous;
            this.awaitMillis = awaitMillis;
        }

        @Override
        public void call(RpcRequest request, StreamObserver<RpcResponse> responseObserver) {
            try {
                rendezvous.countDown();
                rendezvous.await(awaitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            responseObserver.onNext(reply(request, identity));
            responseObserver.onCompleted();
        }
    }

    /**
     * 身份回声服务: 立即返回自身 identity, 用于验证不串台。
     */
    private static final class EchoIdentityService extends GenericRpcServiceGrpc.GenericRpcServiceImplBase {
        private final String identity;

        EchoIdentityService(String identity) {
            this.identity = identity;
        }

        @Override
        public void call(RpcRequest request, StreamObserver<RpcResponse> responseObserver) {
            responseObserver.onNext(reply(request, identity));
            responseObserver.onCompleted();
        }
    }

    private static RpcResponse reply(RpcRequest request, String identity) {
        // sayHello 返回 String, 结果需为 JSON 字符串字面量
        return RpcResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setSuccess(true)
                .setResult("\"" + identity + "\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    private static Server startServer(BindableService service) throws IOException {
        return NettyServerBuilder.forPort(0).addService(service).build().start();
    }

    private static GrpcRpcClient newClient() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        GrpcRpcProperties.ClientConfig config = new GrpcRpcProperties.ClientConfig();
        // 初始 target 仅用于兼容既有单通道字段, callWithTarget 不会使用它
        config.setTarget("localhost:1");
        config.setNegotiationType("plaintext");
        config.setRequestTimeout(30_000L);
        config.setRetryCount(0);
        return new GrpcRpcClient(objectMapper, config);
    }

    /**
     * 正向见证: N 个线程并发经 callWithTarget 打同一地址。
     * 无锁实现下所有调用应同时到达服务端集合点、几乎同时返回, 远早于抢占超时。
     * 若 callWithTarget 退回 synchronized 串行版本, 该用例会以 AssertionFailedError 打红。
     */
    @Test
    void concurrentCallsThroughCallWithTargetDoNotSerialize() throws Exception {
        final int concurrency = 8;
        CountDownLatch rendezvous = new CountDownLatch(concurrency);
        Server server = startServer(new RendezvousService("node", rendezvous, 30_000L));
        String target = "localhost:" + server.getPort();
        GrpcRpcClient client = newClient();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        try {
            CyclicBarrier startBarrier = new CyclicBarrier(concurrency);
            assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < concurrency; i++) {
                    futures.add(pool.submit(() -> {
                        startBarrier.await();
                        results.add(client.callWithTarget(target, TestRpcService.class, "sayHello", "x"));
                        return null;
                    }));
                }
                for (Future<?> f : futures) {
                    f.get();
                }
            });
            assertThat(results).hasSize(concurrency).containsOnly("node");
        } finally {
            pool.shutdownNow();
            client.close();
            server.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    /**
     * 负向注入: 复刻 ConfigurableRpcClient 默认的 synchronized(this){ setTargetAddress + call } 行为,
     * 断言它确实把并发调用串行化 —— 集合点永远凑不齐, 抢占超时以 AssertionFailedError(Failure) 抛出。
     * 这正是无锁化要消除的病灶。
     */
    @Test
    void synchronizedDefaultWouldSerializeAndTimeOut() throws Exception {
        final int concurrency = 4;
        CountDownLatch rendezvous = new CountDownLatch(concurrency);
        Server server = startServer(new RendezvousService("node", rendezvous, 30_000L));
        String target = "localhost:" + server.getPort();
        GrpcRpcClient client = newClient();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        try {
            AssertionFailedError timeout = assertThrows(AssertionFailedError.class, () ->
                    assertTimeoutPreemptively(Duration.ofSeconds(8), () -> {
                        List<Future<?>> futures = new ArrayList<>();
                        for (int i = 0; i < concurrency; i++) {
                            futures.add(pool.submit(() -> {
                                // 等价于默认 default 方法: 持有 client 监视器期间跑完整个 RPC 往返
                                synchronized (client) {
                                    client.setTargetAddress(target);
                                    return client.call(TestRpcService.class, "sayHello", "x");
                                }
                            }));
                        }
                        for (Future<?> f : futures) {
                            f.get();
                        }
                    }));
            assertThat(timeout).hasMessageContaining("timed out");
        } finally {
            pool.shutdownNow();
            client.close();
            server.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    /**
     * 不串台: 16 线程并发, 每轮交替打两个不同地址的服务实例,
     * 每次都断言拿回的是所打地址对应实例的身份。原锁存在的理由就是防止 target 被并发覆盖导致串台,
     * 无锁版按地址各持 Channel, 必须证明请求始终落到正确实例。
     */
    @Test
    void concurrentCallsToDifferentTargetsDoNotCrossTalk() throws Exception {
        Server alpha = startServer(new EchoIdentityService("alpha"));
        Server beta = startServer(new EchoIdentityService("beta"));
        String alphaTarget = "localhost:" + alpha.getPort();
        String betaTarget = "localhost:" + beta.getPort();
        GrpcRpcClient client = newClient();

        final int threads = 16;
        final int rounds = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CyclicBarrier startBarrier = new CyclicBarrier(threads);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    startBarrier.await();
                    for (int r = 0; r < rounds; r++) {
                        boolean useAlpha = (r % 2 == 0);
                        String target = useAlpha ? alphaTarget : betaTarget;
                        String expected = useAlpha ? "alpha" : "beta";
                        String got = client.callWithTarget(target, TestRpcService.class, "sayHello", "x");
                        if (!expected.equals(got)) {
                            return Boolean.FALSE;
                        }
                    }
                    return Boolean.TRUE;
                }));
            }
            for (Future<Boolean> f : futures) {
                assertThat(f.get(30, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            pool.shutdownNow();
            client.close();
            alpha.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
            beta.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }
}
