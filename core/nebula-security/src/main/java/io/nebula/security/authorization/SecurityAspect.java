package io.nebula.security.authorization;

import io.nebula.security.annotation.RequiresPermission;
import io.nebula.security.annotation.RequiresRole;
import io.nebula.security.authentication.Authentication;
import io.nebula.security.authentication.GrantedAuthority;
import io.nebula.security.authentication.SecurityContext;
import io.nebula.security.authentication.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全注解 AOP 切面。
 * <p>
 * 同时支持方法级与类级注解(注解 @Target 本就含 TYPE)：切点匹配 {@code @annotation} 与 {@code @within}，
 * 并在 advice 内按"方法优先、再目标类"解析注解——修复此前切点只写 {@code @annotation} 导致
 * 类级注解静默失效的授权绕过。
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Aspect
@Component
public class SecurityAspect {

    @Before("@annotation(io.nebula.security.annotation.RequiresAuthentication)"
            + " || @within(io.nebula.security.annotation.RequiresAuthentication)")
    public void checkAuthentication(JoinPoint joinPoint) {
        requireAuthenticated();
    }

    @Before("@annotation(io.nebula.security.annotation.RequiresPermission)"
            + " || @within(io.nebula.security.annotation.RequiresPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        Authentication auth = requireAuthenticated();

        RequiresPermission requiresPermission = findAnnotation(joinPoint, RequiresPermission.class);
        if (requiresPermission == null) {
            return;
        }

        String[] requiredPermissions = requiresPermission.value();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            throw new SecurityException("User has no permissions");
        }

        Set<String> userPermissions = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean hasPermission;
        if (requiresPermission.logical() == RequiresPermission.Logical.AND) {
            hasPermission = userPermissions.containsAll(Arrays.asList(requiredPermissions));
        } else {
            hasPermission = Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);
        }

        if (!hasPermission) {
            throw new SecurityException("User lacks required permissions: " + String.join(", ", requiredPermissions));
        }
    }

    @Before("@annotation(io.nebula.security.annotation.RequiresRole)"
            + " || @within(io.nebula.security.annotation.RequiresRole)")
    public void checkRole(JoinPoint joinPoint) {
        Authentication auth = requireAuthenticated();

        RequiresRole requiresRole = findAnnotation(joinPoint, RequiresRole.class);
        if (requiresRole == null) {
            return;
        }

        if (!(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new SecurityException("Invalid principal type");
        }

        Set<String> userRoles = principal.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            throw new SecurityException("User has no roles");
        }

        String[] requiredRoles = requiresRole.value();
        boolean hasRole;
        if (requiresRole.logical() == RequiresPermission.Logical.AND) {
            hasRole = userRoles.containsAll(Arrays.asList(requiredRoles));
        } else {
            hasRole = Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
        }

        if (!hasRole) {
            throw new SecurityException("User lacks required roles: " + String.join(", ", requiredRoles));
        }
    }

    /**
     * 校验已认证，返回认证信息。
     */
    private Authentication requireAuthenticated() {
        Authentication auth = SecurityContext.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        return auth;
    }

    /**
     * 解析目标注解：方法优先，其次目标类/声明类。支持类级注解。
     */
    private <A extends Annotation> A findAnnotation(JoinPoint joinPoint, Class<A> annotationType) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (annotation != null) {
            return annotation;
        }
        Class<?> targetClass = joinPoint.getTarget() != null
                ? joinPoint.getTarget().getClass()
                : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, annotationType);
    }
}
