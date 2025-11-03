package io.nebula.security.authorization;

import io.nebula.security.annotation.RequiresAuthentication;
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
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全注解AOP切面
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Aspect
@Component
public class SecurityAspect {
    
    /**
     * 检查认证
     */
    @Before("@annotation(requiresAuthentication)")
    public void checkAuthentication(JoinPoint joinPoint, RequiresAuthentication requiresAuthentication) {
        Authentication auth = SecurityContext.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
    }
    
    /**
     * 检查权限
     */
    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Authentication auth = SecurityContext.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
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
            // 需要拥有所有权限
            hasPermission = userPermissions.containsAll(Arrays.asList(requiredPermissions));
        } else {
            // 需要拥有任意一个权限
            hasPermission = Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        }
        
        if (!hasPermission) {
            throw new SecurityException("User lacks required permissions: " + 
                    String.join(", ", requiredPermissions));
        }
    }
    
    /**
     * 检查角色
     */
    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint joinPoint, RequiresRole requiresRole) {
        Authentication auth = SecurityContext.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        if (!(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new SecurityException("Invalid principal type");
        }
        
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Set<String> userRoles = principal.getRoles();
        
        if (userRoles == null || userRoles.isEmpty()) {
            throw new SecurityException("User has no roles");
        }
        
        String[] requiredRoles = requiresRole.value();
        boolean hasRole;
        
        if (requiresRole.logical() == RequiresPermission.Logical.AND) {
            // 需要拥有所有角色
            hasRole = userRoles.containsAll(Arrays.asList(requiredRoles));
        } else {
            // 需要拥有任意一个角色
            hasRole = Arrays.stream(requiredRoles)
                    .anyMatch(userRoles::contains);
        }
        
        if (!hasRole) {
            throw new SecurityException("User lacks required roles: " + 
                    String.join(", ", requiredRoles));
        }
    }
}

