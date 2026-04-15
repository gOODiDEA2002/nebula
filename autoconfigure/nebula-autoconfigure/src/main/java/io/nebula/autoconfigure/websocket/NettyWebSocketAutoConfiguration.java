package io.nebula.autoconfigure.websocket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * Netty WebSocket 统一自动配置入口
 * <p>
 * 当 classpath 中存在 {@code nebula-websocket-netty} 模块时自动激活，
 * 实际配置逻辑委托给模块内部的 {@code NettyWebSocketAutoConfiguration}。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.nebula.websocket.netty.config.NettyWebSocketAutoConfiguration")
@Import(io.nebula.websocket.netty.config.NettyWebSocketAutoConfiguration.class)
public class NettyWebSocketAutoConfiguration {
}
