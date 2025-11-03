package io.nebula.security.annotation;

import java.lang.annotation.*;

/**
 * 需要认证注解
 * 
 * 标记在方法或类上,要求用户必须登录
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresAuthentication {
}

