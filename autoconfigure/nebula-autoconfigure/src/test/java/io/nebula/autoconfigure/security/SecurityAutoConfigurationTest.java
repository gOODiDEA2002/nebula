package io.nebula.autoconfigure.security;

import io.nebula.security.authentication.JwtAuthenticationFilter;
import io.nebula.security.authorization.SecurityAspect;
import io.nebula.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证安全自动装配的**生产路径**：JwtAuthenticationFilter 的注册受 opt-in 属性控制。
 * <p>
 * 对应 Codex 审查修正——T-A1-3 的验证必须覆盖真实注册点(SecurityAutoConfiguration 在 autoconfigure 模块)，
 * 而不能只测 core 类。
 */
class SecurityAutoConfigurationTest {

    // secret 需 >= 32 字符, 满足 DefaultJwtService 启动校验
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
            .withPropertyValues("nebula.security.jwt.secret=0123456789012345678901234567890123456789");

    @Test
    void jwtFilterNotRegisteredByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(JwtService.class);
            assertThat(context).hasSingleBean(SecurityAspect.class);
            // 默认关闭, 不影响存量应用
            assertThat(context).doesNotHaveBean(JwtAuthenticationFilter.class);
        });
    }

    @Test
    void jwtFilterRegisteredWhenEnabled() {
        runner.withPropertyValues("nebula.security.jwt.filter.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(JwtAuthenticationFilter.class));
    }
}
