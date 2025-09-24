package io.nebula.web.mask;

import java.lang.annotation.*;

/**
 * 敏感数据注解
 * 用于标记需要脱敏的字段
 * 
 * @author nebula
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveData {
    
    /**
     * 脱敏策略类型
     */
    MaskType type() default MaskType.CUSTOM;
    
    /**
     * 自定义脱敏策略名称
     */
    String strategy() default "";
    
    /**
     * 是否启用脱敏（可用于动态控制）
     */
    boolean enabled() default true;
}
