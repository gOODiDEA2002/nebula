package io.nebula.autoconfigure.diagnostic;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nebula 框架启动摘要监听器
 * <p>
 * 在应用启动完成后，自动收集所有 {@link NebulaComponentSummary} Bean，
 * 按分组渲染并输出框架配置摘要。
 * <p>
 * 本类是纯渲染器，不包含任何组件特定的逻辑。
 * 组件信息通过各 AutoConfiguration 类中的 NebulaComponentSummary @Bean 方法注册。
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration
public class NebulaStartupSummaryListener {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> nebulaStartupSummary(
            Environment environment,
            List<NebulaComponentSummary> summaries) {
        return event -> printStartupSummary(environment, summaries);
    }

    private void printStartupSummary(Environment environment,
            List<NebulaComponentSummary> summaries) {
        StringBuilder sb = new StringBuilder();
        String ls = System.lineSeparator();

        sb.append(ls);
        sb.append("=".repeat(70)).append(ls);
        sb.append("                    NEBULA FRAMEWORK STARTUP SUMMARY                   ").append(ls);
        sb.append("=".repeat(70)).append(ls);

        // 框架基本信息
        appendSection(sb, "Framework Info");
        appendEntry(sb, "Version", "2.0.1-SNAPSHOT");
        appendEntry(sb, "Profile", getActiveProfiles(environment));

        // 按 group 分组、组内按 order 排序
        Map<String, List<NebulaComponentSummary>> grouped = groupByGroup(summaries);

        for (Map.Entry<String, List<NebulaComponentSummary>> entry : grouped.entrySet()) {
            appendSection(sb, entry.getKey());
            for (NebulaComponentSummary cs : entry.getValue()) {
                renderComponent(sb, cs);
            }
        }

        sb.append("=".repeat(70)).append(ls);

        log.info(sb.toString());
    }

    /**
     * 渲染单个组件: 名称 + 状态 + 配置详情
     */
    private void renderComponent(StringBuilder sb, NebulaComponentSummary cs) {
        appendEntry(sb, cs.name(), cs.isEnabled() ? "ENABLED" : "DISABLED");
        if (cs.isEnabled()) {
            for (Map.Entry<String, String> detail : cs.configDetails().entrySet()) {
                appendEntry(sb, "  " + detail.getKey(), detail.getValue());
            }
        }
    }

    /**
     * 按 group 分组，保持插入顺序，组内按 order 排序
     */
    private Map<String, List<NebulaComponentSummary>> groupByGroup(
            List<NebulaComponentSummary> summaries) {
        // 先按 order 全局排序，再按 group 分组保持顺序
        List<NebulaComponentSummary> sorted = new ArrayList<>(summaries);
        sorted.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        return sorted.stream()
                .collect(Collectors.groupingBy(
                        NebulaComponentSummary::group,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void appendSection(StringBuilder sb, String title) {
        sb.append(String.format("%n  [%s]%n", title));
    }

    private void appendEntry(StringBuilder sb, String key, String value) {
        sb.append(String.format("    %-20s : %s%n", key, value != null ? value : "N/A"));
    }

    private String getActiveProfiles(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(", ", profiles);
    }
}
