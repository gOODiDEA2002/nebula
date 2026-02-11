package io.nebula.core.common.diagnostic;

import java.util.Map;

/**
 * Nebula 组件摘要信息接口
 * <p>
 * 各 AutoConfiguration 通过注册此接口的 Bean 来贡献组件信息，
 * 启动摘要和诊断端点会自动收集并展示。
 * <p>
 * 设计参考 Spring Boot 的 {@code HealthIndicator} / {@code InfoContributor} 模式。
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public interface NebulaComponentSummary extends org.springframework.core.Ordered {

    /**
     * 组件所属分组，用于启动摘要中的分组标题
     * 例如: "Service Discovery", "RPC", "Data", "AI"
     */
    String group();

    /**
     * 组件名称
     * 例如: "Nacos", "HTTP RPC", "Cache (Redis)"
     */
    String name();

    /**
     * 组件是否启用
     */
    boolean isEnabled();

    /**
     * 关键配置详情 (key -> value)
     * <p>
     * 仅在 isEnabled() 为 true 时展示。
     * 禁止包含密码等敏感信息。
     *
     * @return 有序的配置键值对
     */
    Map<String, String> configDetails();
}
