package io.nebula.web.autoconfigure;

import io.nebula.web.config.JacksonConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Nebula Web 自动配置入口
 * 通过 @Import 引入各功能域的子配置类，保持职责单一
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(WebProperties.class)
@Import({
    JacksonConfig.class,
    WebCoreAutoConfiguration.class,
    WebRateLimitAutoConfiguration.class,
    WebCacheAutoConfiguration.class,
    WebAuthAutoConfiguration.class,
    WebMonitorAutoConfiguration.class
})
public class WebAutoConfiguration {
}
