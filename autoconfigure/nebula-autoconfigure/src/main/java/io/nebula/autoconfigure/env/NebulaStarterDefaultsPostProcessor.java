package io.nebula.autoconfigure.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 扫描 classpath 上所有 Starter JAR 中的 META-INF/nebula-defaults.properties，
 * 以最低优先级注入到 Environment，确保用户 application.yml 始终可以覆盖。
 * <p>
 * 用途：在所有模块默认 matchIfMissing=false 的前提下，让各 Starter
 * 声明自己需要启用的模块（nebula.xxx.enabled=true），
 * 使引入 Starter 依赖即可开箱即用。
 */
public class NebulaStarterDefaultsPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULTS_LOCATION = "classpath*:META-INF/nebula-defaults.properties";
    private static final String PROPERTY_SOURCE_NAME = "nebula-starter-defaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                        SpringApplication application) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(DEFAULTS_LOCATION);

            Map<String, Object> defaults = new LinkedHashMap<>();
            for (Resource resource : resources) {
                Properties props = new Properties();
                props.load(resource.getInputStream());
                props.forEach((key, value) -> defaults.putIfAbsent(key.toString(), value));
            }

            if (!defaults.isEmpty()) {
                environment.getPropertySources().addLast(
                        new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
            }
        } catch (IOException ignored) {
        }
    }
}
