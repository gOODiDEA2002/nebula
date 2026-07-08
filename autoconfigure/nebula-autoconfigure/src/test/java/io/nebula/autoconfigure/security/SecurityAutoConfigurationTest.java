package io.nebula.autoconfigure.security;

import io.nebula.security.authentication.JwtAuthenticationFilter;
import io.nebula.security.authorization.SecurityAspect;
import io.nebula.security.config.SecurityProperties;
import io.nebula.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityAutoConfiguration 三态条件测试 + JwtAuthenticationFilter opt-in 验证。
 * <p>
 * (1) enabled=true（matchIfMissing=true, 默认启用）→ JwtService/SecurityAspect 存在
 * (2) enabled=false → 无任何 Bean
 * (3) JwtService 类缺失 → 配置不加载
 * (4/5) JwtAuthenticationFilter 的 opt-in 开关
 */
class SecurityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
            .withPropertyValues("nebula.security.jwt.secret=0123456789012345678901234567890123456789");

    @Test
    void defaultEnabled_hasJwtServiceAndAspect() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(JwtService.class);
            assertThat(context).hasSingleBean(SecurityAspect.class);
            assertThat(context).doesNotHaveBean(JwtAuthenticationFilter.class);
        });
    }

    @Test
    void explicitlyDisabled_noBeans() {
        runner.withPropertyValues("nebula.security.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JwtService.class);
                    assertThat(context).doesNotHaveBean(SecurityAspect.class);
                });
    }

    @Test
    void missingClass_noBeans() {
        runner.withClassLoader(new FilteredClassLoader(SecurityProperties.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JwtService.class);
                    assertThat(context).doesNotHaveBean(SecurityAspect.class);
                });
    }

    @Test
    void jwtFilterRegisteredWhenOptInEnabled() {
        runner.withPropertyValues("nebula.security.jwt.filter.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(JwtAuthenticationFilter.class));
    }

    @Test
    void jwtFilterRegisteredWhenWebAuthEnabled() {
        runner.withPropertyValues("nebula.web.auth.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(JwtAuthenticationFilter.class));
    }
}
