package io.nebula.autoconfigure.rpc;

import io.nebula.discovery.core.LoadBalanceStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证负载均衡策略配置值到枚举的映射。
 * 修复前 {@code valueOf("WEIGHTED")}(配置 weighted 时)会抛 IllegalArgumentException 导致启动崩溃。
 */
class RpcLoadBalanceStrategyTest {

    private final RpcDiscoveryAutoConfiguration config = new RpcDiscoveryAutoConfiguration();

    @Test
    void weightedAliasMapsToWeightedRandomWithoutCrash() {
        assertThatCode(() -> config.resolveStrategy("weighted")).doesNotThrowAnyException();
        assertThat(config.resolveStrategy("weighted")).isEqualTo(LoadBalanceStrategy.WEIGHTED_RANDOM);
    }

    @Test
    void mapsAllSupportedValues() {
        assertThat(config.resolveStrategy("round_robin")).isEqualTo(LoadBalanceStrategy.ROUND_ROBIN);
        assertThat(config.resolveStrategy("random")).isEqualTo(LoadBalanceStrategy.RANDOM);
        assertThat(config.resolveStrategy("WEIGHTED_RANDOM")).isEqualTo(LoadBalanceStrategy.WEIGHTED_RANDOM);
        assertThat(config.resolveStrategy("weighted_round_robin")).isEqualTo(LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN);
    }

    @Test
    void unknownOrBlankFallsBackToRoundRobin() {
        assertThat(config.resolveStrategy("bogus")).isEqualTo(LoadBalanceStrategy.ROUND_ROBIN);
        assertThat(config.resolveStrategy("")).isEqualTo(LoadBalanceStrategy.ROUND_ROBIN);
        assertThat(config.resolveStrategy(null)).isEqualTo(LoadBalanceStrategy.ROUND_ROBIN);
    }
}
