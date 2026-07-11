package io.nebula.data.cache.manager;

import io.nebula.data.cache.manager.impl.LocalCacheManager;
import io.nebula.data.cache.sync.CacheInvalidationBroadcaster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多级缓存 CD-10 可靠性行为测试:
 * 击穿防护(single-flight)、穿透防护(空值哨兵)、跨节点失效(广播/接收)。
 *
 * <p>L1/L2 均用 LocalCacheManager 充当(接口行为一致), 不依赖 Redis 环境。</p>
 */
class MultiLevelCacheManagerTest {

    private LocalCacheManager l1;
    private LocalCacheManager l2;

    @BeforeEach
    void setUp() {
        l1 = new LocalCacheManager();
        l2 = new LocalCacheManager();
    }

    @AfterEach
    void tearDown() {
        l1.destroy();
        l2.destroy();
    }

    private MultiLevelCacheManager manager(MultiLevelCacheConfig config) {
        return new MultiLevelCacheManager(l1, l2, config);
    }

    @Test
    @DisplayName("击穿防护: 并发同 key miss 只回源一次")
    void singleFlightLoadsOnlyOnce() throws Exception {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.defaultConfig());
        AtomicInteger loadCount = new AtomicInteger();
        int threads = 16;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<java.util.concurrent.Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return manager.getOrSet("hot-key", String.class, () -> {
                    loadCount.incrementAndGet();
                    try {
                        // 拉长回源耗时, 确保并发线程都进入等待路径
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "loaded";
                }, Duration.ofMinutes(1));
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        for (var future : results) {
            assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("loaded");
        }
        pool.shutdown();

        assertThat(loadCount.get()).as("并发 miss 应只有一个线程回源").isEqualTo(1);
    }

    @Test
    @DisplayName("击穿防护: 首航回源抛异常时跟随线程自行回源, 不被级联拖垮")
    void followerFallsBackWhenLeaderFails() throws Exception {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.defaultConfig());
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch leaderStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        var leader = pool.submit(() -> {
            try {
                return manager.getOrSet("fail-key", String.class, () -> {
                    attempts.incrementAndGet();
                    leaderStarted.countDown();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new IllegalStateException("回源失败");
                }, Duration.ofMinutes(1));
            } catch (IllegalStateException e) {
                return "leader-failed";
            }
        });

        assertThat(leaderStarted.await(5, TimeUnit.SECONDS)).isTrue();
        var follower = pool.submit(() -> manager.getOrSet("fail-key", String.class, () -> {
            attempts.incrementAndGet();
            return "recovered";
        }, Duration.ofMinutes(1)));

        assertThat(leader.get(5, TimeUnit.SECONDS)).isEqualTo("leader-failed");
        assertThat(follower.get(5, TimeUnit.SECONDS)).isEqualTo("recovered");
        pool.shutdown();
    }

    @Test
    @DisplayName("穿透防护: 启用空值哨兵后, 窗口内不存在的 key 不重复回源")
    void nullSentinelPreventsRepeatedLoads() {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.builder()
                .nullCachingEnabled(true)
                .nullValueTtl(Duration.ofMinutes(1))
                .build());
        AtomicInteger loadCount = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            String result = manager.getOrSet("missing-key", String.class, () -> {
                loadCount.incrementAndGet();
                return null;
            }, Duration.ofMinutes(10));
            assertThat(result).isNull();
        }

        assertThat(loadCount.get()).as("哨兵窗口内只应回源一次").isEqualTo(1);
    }

    @Test
    @DisplayName("穿透防护关闭(默认): null 结果不缓存, 每次都回源")
    void nullNotCachedByDefault() {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.defaultConfig());
        AtomicInteger loadCount = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            assertThat(manager.getOrSet("missing-key", String.class, () -> {
                loadCount.incrementAndGet();
                return null;
            }, Duration.ofMinutes(10))).isNull();
        }

        assertThat(loadCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("穿透防护: 数据出现后哨兵被覆盖, 恢复正常读取")
    void sentinelOverwrittenWhenDataAppears() {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.builder()
                .nullCachingEnabled(true)
                .nullValueTtl(Duration.ofMinutes(1))
                .build());

        assertThat(manager.getOrSet("k", String.class, () -> null, Duration.ofMinutes(10))).isNull();
        // 数据创建后显式 set(典型业务路径: 创建记录时写缓存)
        manager.set("k", "created", Duration.ofMinutes(10));

        assertThat(manager.getOrSet("k", String.class, () -> "should-not-load", Duration.ofMinutes(10)))
                .isEqualTo("created");
    }

    @Test
    @DisplayName("短 TTL: L1 不得比调用方指定的过期时间更长")
    void l1TtlDoesNotOutliveRequestedTtl() throws Exception {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.builder()
                .l1MinTtl(Duration.ofMinutes(1))
                .l1TtlRatio(0.5)
                .build());

        manager.set("short-lived", "value", Duration.ofMillis(100));
        Thread.sleep(180);

        assertThat(l1.get("short-lived", String.class)).isEmpty();
        assertThat(l2.get("short-lived", String.class)).isEmpty();
        assertThat(manager.get("short-lived", String.class)).isEmpty();
    }

    @Test
    @DisplayName("跨节点失效: 写路径广播 evict/clear, 收到远端通知只动 L1")
    void invalidationBroadcastAndRemoteHandling() {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.defaultConfig());
        List<String> evicted = new ArrayList<>();
        AtomicInteger clears = new AtomicInteger();
        manager.setInvalidationBroadcaster(new CacheInvalidationBroadcaster() {
            @Override
            public void publishEvict(String key) {
                evicted.add(key);
            }

            @Override
            public void publishClear() {
                clears.incrementAndGet();
            }
        });

        manager.set("a", "1");
        manager.delete("a");
        manager.clear();
        assertThat(evicted).containsExactly("a", "a");
        assertThat(clears.get()).isEqualTo(1);

        // 收到远端 evict: 仅驱逐 L1, L2 保留(其他节点已写入新值), 且不得再广播
        evicted.clear();
        manager.set("b", "old");
        manager.onRemoteEvict("b");
        assertThat(l1.get("b", String.class)).isEmpty();
        assertThat(l2.get("b", String.class)).contains("old");
        assertThat(evicted).as("处理远端通知不得回发广播").containsExactly("b");

        // 收到远端 clear: 仅清空 L1
        manager.set("c", "v");
        manager.onRemoteClear();
        assertThat(l1.get("c", String.class)).isEmpty();
        assertThat(l2.get("c", String.class)).contains("v");
    }

    @Test
    @DisplayName("未注入广播器时写路径正常工作(单节点部署)")
    void worksWithoutBroadcaster() {
        MultiLevelCacheManager manager = manager(MultiLevelCacheConfig.defaultConfig());
        manager.set("k", "v");
        assertThat(manager.get("k", String.class)).contains("v");
        assertThat(manager.delete("k")).isTrue();
        manager.clear();
    }
}
