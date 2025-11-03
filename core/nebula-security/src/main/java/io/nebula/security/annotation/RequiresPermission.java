package io.nebula.security.annotation;

import java.lang.annotation.*;

/**
 * 权限检查注解
 * 
 * 标记在方法上,自动检查用户是否拥有指定权限
 * 
 * 使用示例:
 * <pre>{@code
 * @RequiresPermission("order:delete")
 * public void deleteOrder(Long orderId) {
 *     // 业务逻辑
 * }
 * 
 * @RequiresPermission(value = {"order:create", "order:update"}, logical = Logical.OR)
 * public void saveOrder(Order order) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    
    /**
     * 所需权限列表
     */
    String[] value();
    
    /**
     * 逻辑关系
     * - AND: 需要拥有所有权限
     * - OR: 需要拥有任意一个权限
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND, OR
    }
}

