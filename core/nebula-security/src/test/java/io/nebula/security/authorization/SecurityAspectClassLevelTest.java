package io.nebula.security.authorization;

import io.nebula.security.annotation.RequiresRole;
import io.nebula.security.authentication.JwtAuthenticationToken;
import io.nebula.security.authentication.SecurityContext;
import io.nebula.security.authentication.SimpleGrantedAuthority;
import io.nebula.security.authentication.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 {@link SecurityAspect} 现在会强制**类级**安全注解。
 * <p>
 * 修复前切点只有 {@code @annotation}，加在类上的 {@code @RequiresRole} 静默失效(授权绕过)——
 * 那时 {@code hello()} 无论是否认证都能返回。加入 {@code @within} 后，下面"未认证/角色不符"两个用例才会抛异常。
 */
class SecurityAspectClassLevelTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clearAuthentication();
    }

    private SecuredService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SecuredService());
        factory.addAspect(new SecurityAspect());
        return factory.getProxy();
    }

    @Test
    void classLevelRoleAllowsWhenRolePresent() {
        authenticateWithRoles("ADMIN");
        assertThat(proxy().hello()).isEqualTo("ok");
    }

    @Test
    void classLevelRoleRejectsWhenRoleMissing() {
        authenticateWithRoles("USER");
        SecuredService service = proxy();
        assertThatThrownBy(service::hello).isInstanceOf(SecurityException.class);
    }

    @Test
    void classLevelRoleRejectsWhenUnauthenticated() {
        SecurityContext.clearAuthentication();
        SecuredService service = proxy();
        assertThatThrownBy(service::hello).isInstanceOf(SecurityException.class);
    }

    private void authenticateWithRoles(String... roles) {
        UserPrincipal principal = new UserPrincipal(1L, "u",
                List.of(new SimpleGrantedAuthority("dummy")));
        Set<String> roleSet = new HashSet<>(List.of(roles));
        principal.setRoles(roleSet);
        SecurityContext.setAuthentication(new JwtAuthenticationToken("t", principal, principal.getAuthorities()));
    }

    /**
     * 类级 @RequiresRole；具体类(无接口)使 AspectJProxyFactory 走 CGLIB，@within 可稳定匹配。
     */
    @RequiresRole("ADMIN")
    static class SecuredService {
        public String hello() {
            return "ok";
        }
    }
}
