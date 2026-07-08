package io.nebula.autoconfigure.security;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * JWT 认证过滤器注册条件：以下任一成立即注册 JwtAuthenticationFilter。
 * <ul>
 *   <li>{@code nebula.security.jwt.filter.enabled=true}（原 opt-in 独立开关，保留为覆盖手段）</li>
 *   <li>{@code nebula.web.auth.enabled=true}（web 层认证拦截器需要 Filter 预填充 SecurityContext）</li>
 * </ul>
 */
class JwtFilterEnabledCondition extends AnyNestedCondition {

    JwtFilterEnabledCondition() {
        super(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(prefix = "nebula.security.jwt.filter", name = "enabled", havingValue = "true")
    static class SecurityFilterExplicit {}

    @ConditionalOnProperty(prefix = "nebula.web.auth", name = "enabled", havingValue = "true")
    static class WebAuthEnabled {}
}
