package io.nebula.rpc.http.client;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 HttpRpcClient 的目标地址随请求(per-thread)传递，不改写共享 baseUrl——
 * 修复"并发调用不同实例时共享 baseUrl 被互相覆盖、请求打到错误实例"。
 */
class HttpRpcClientTargetIsolationTest {

    private HttpRpcClient newClient() {
        return new HttpRpcClient(mock(RestClient.class), "http://default:8080",
                Runnable::run, JsonMapper.builder().build());
    }

    @Test
    void targetOverrideIsPerRequestAndClearedAfter() {
        HttpRpcClient client = newClient();

        assertThat(client.effectiveBaseUrl()).isEqualTo("http://default:8080");

        String seen = client.runWithTarget("http://target-a:9090", client::effectiveBaseUrl);
        assertThat(seen).isEqualTo("http://target-a:9090");

        // 调用结束后必须恢复默认(ThreadLocal 已清理)
        assertThat(client.effectiveBaseUrl()).isEqualTo("http://default:8080");
    }

    @Test
    void concurrentCallsToDifferentTargetsDoNotBleed() throws Exception {
        HttpRpcClient client = newClient();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean aOk = new AtomicBoolean();
        AtomicBoolean bOk = new AtomicBoolean();

        Runnable taskA = () -> client.runWithTarget("http://a:1", () -> {
            await(start);
            // 线程 A 在其请求内看到的必须始终是自己的 target，即便 B 并发设置了不同 target
            boolean ok = true;
            for (int i = 0; i < 1000; i++) {
                ok &= "http://a:1".equals(client.effectiveBaseUrl());
            }
            aOk.set(ok);
            return null;
        });
        Runnable taskB = () -> client.runWithTarget("http://b:2", () -> {
            await(start);
            boolean ok = true;
            for (int i = 0; i < 1000; i++) {
                ok &= "http://b:2".equals(client.effectiveBaseUrl());
            }
            bOk.set(ok);
            return null;
        });

        pool.submit(taskA);
        pool.submit(taskB);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(aOk).isTrue();
        assertThat(bOk).isTrue();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
