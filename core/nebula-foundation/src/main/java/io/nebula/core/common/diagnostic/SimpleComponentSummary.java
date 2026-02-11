package io.nebula.core.common.diagnostic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link NebulaComponentSummary} 的通用实现
 * <p>
 * AutoConfiguration 中可通过构造函数快速创建:
 * 
 * <pre>
 * new SimpleComponentSummary("Data", "Cache (Redis)", true, 0,
 *         Map.of("Type", "redis", "Host", "localhost:6379"));
 * </pre>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public class SimpleComponentSummary implements NebulaComponentSummary {

    private final String group;
    private final String name;
    private final boolean enabled;
    private final int order;
    private final Map<String, String> configDetails;

    /**
     * @param group         组件分组名
     * @param name          组件名称
     * @param enabled       是否启用
     * @param order         排序权重 (值越小越靠前)
     * @param configDetails 关键配置键值对 (不含敏感信息)
     */
    public SimpleComponentSummary(String group, String name, boolean enabled,
            int order, Map<String, String> configDetails) {
        this.group = group;
        this.name = name;
        this.enabled = enabled;
        this.order = order;
        this.configDetails = configDetails != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(configDetails))
                : Collections.emptyMap();
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> configDetails() {
        return configDetails;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
