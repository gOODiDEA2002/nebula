package io.nebula.web.performance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link PerformanceMetrics#reset()} 真实清零。
 * 修复前 DefaultPerformanceMonitor.resetMetrics() 是空实现(只打日志), /performance/reset 谎报成功。
 */
class PerformanceMetricsResetTest {

    @Test
    void resetZeroesAllMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.addResponseTime(150);
        metrics.incrementSlowRequestCount();
        metrics.incrementActiveRequests();
        metrics.incrementStatusCount(200);
        metrics.incrementPathCount("/api/x");

        // 前置: 确有数据
        assertThat(metrics.getTotalResponseTime()).isEqualTo(150);
        assertThat(metrics.getMaxResponseTime()).isEqualTo(150);
        assertThat(metrics.getStatusCounts()).isNotEmpty();

        metrics.reset();

        assertThat(metrics.getTotalResponseTime()).isZero();
        assertThat(metrics.getMaxResponseTime()).isZero();
        assertThat(metrics.getMinResponseTime()).isZero();
        assertThat(metrics.getSlowRequestCount()).isZero();
        assertThat(metrics.getActiveRequests()).isZero();
        assertThat(metrics.getStatusCounts()).isEmpty();
        assertThat(metrics.getPathCounts()).isEmpty();
    }

    @Test
    void monitorResetDelegatesToMetrics() {
        DefaultPerformanceMonitor monitor = new DefaultPerformanceMonitor(1000);
        monitor.getMetrics().addResponseTime(80);
        assertThat(monitor.getMetrics().getTotalResponseTime()).isEqualTo(80);

        monitor.resetMetrics();

        assertThat(monitor.getMetrics().getTotalResponseTime()).isZero();
    }
}
