package io.nebula.security.annotation;

import java.lang.annotation.*;

/**
 * 角色检查注解
 * 
 * 标记在方法上,自动检查用户是否拥有指定角色
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    
    /**
     * 所需角色列表
     */
    String[] value();
    
    /**
     * 逻辑关系
     */
    RequiresPermission.Logical logical() default RequiresPermission.Logical.AND;
}

