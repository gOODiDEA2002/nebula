package io.nebula.autoconfigure.websocket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * Spring WebSocket 统一自动配置入口
 * <p>
 * 当 classpath 中存在 {@code nebula-websocket-spring} 模块时自动激活，
 * 实际配置逻辑委托给模块内部的 {@code WebSocketAutoConfiguration}。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.nebula.websocket.spring.config.WebSocketAutoConfiguration")
@Import(io.nebula.websocket.spring.config.WebSocketAutoConfiguration.class)
public class SpringWebSocketAutoConfiguration {
}
