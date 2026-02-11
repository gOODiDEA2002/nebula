package io.nebula.autoconfigure.diagnostic;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nebula 框架诊断端点
 * <p>
 * 提供框架运行状态的诊断信息，支持通过 Actuator 访问。
 * 复用 {@link NebulaComponentSummary} 贡献者来展示所有组件状态。
 * <p>
 * 访问路径: /actuator/nebula-diagnostic
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class NebulaDiagnosticEndpoint {

    @Bean
    public NebulaDiagnosticEndpointBean nebulaDiagnosticEndpointBean(
            Environment environment,
            List<NebulaComponentSummary> summaries) {
        return new NebulaDiagnosticEndpointBean(environment, summaries);
    }

    @Endpoint(id = "nebula-diagnostic")
    public static class NebulaDiagnosticEndpointBean {

        private final Environment environment;
        private final List<NebulaComponentSummary> summaries;

        public NebulaDiagnosticEndpointBean(Environment environment,
                List<NebulaComponentSummary> summaries) {
            this.environment = environment;
            this.summaries = summaries;
        }

        @ReadOperation
        public Map<String, Object> diagnostic() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("timestamp", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("framework", getFrameworkInfo());
            result.put("components", getComponentsInfo());

            return result;
        }

        private Map<String, Object> getFrameworkInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", "Nebula Framework");
            info.put("version", "2.0.1-SNAPSHOT");
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("springBootVersion", org.springframework.boot.SpringBootVersion.getVersion());
            info.put("activeProfiles", environment.getActiveProfiles().length > 0
                    ? environment.getActiveProfiles()
                    : new String[] { "default" });
            return info;
        }

        /**
         * 通过 NebulaComponentSummary 贡献者构建组件诊断信息
         */
        private Map<String, Object> getComponentsInfo() {
            // 排序后按 group 分组
            List<NebulaComponentSummary> sorted = new ArrayList<>(summaries);
            sorted.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

            Map<String, List<Map<String, Object>>> grouped = sorted.stream()
                    .collect(Collectors.groupingBy(
                            NebulaComponentSummary::group,
                            LinkedHashMap::new,
                            Collectors.mapping(this::toComponentMap, Collectors.toList())));

            // 转为 Map<String, Object> 减少嵌套层级
            Map<String, Object> result = new LinkedHashMap<>();
            grouped.forEach((group, components) -> {
                if (components.size() == 1) {
                    result.put(group, components.get(0));
                } else {
                    result.put(group, components);
                }
            });
            return result;
        }

        private Map<String, Object> toComponentMap(NebulaComponentSummary cs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", cs.name());
            map.put("enabled", cs.isEnabled());
            if (cs.isEnabled() && !cs.configDetails().isEmpty()) {
                map.put("config", cs.configDetails());
            }
            return map;
        }
    }
}
