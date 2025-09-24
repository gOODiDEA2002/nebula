package io.nebula.data.persistence.readwrite.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 读数据源注解
 * 标记方法或类使用读数据源（从库）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadDataSource {
    
    /**
     * 集群名称
     * 默认为 "default"
     */
    String cluster() default "default";
    
    /**
     * 是否强制使用读数据源
     * 如果为true，即使在事务中也使用读数据源
     * 如果为false，在事务中会使用写数据源以保证一致性
     */
    boolean force() default false;
    
    /**
     * 优先级
     * 数字越大优先级越高，当方法和类都有注解时，优先级高的生效
     */
    int priority() default 0;
    
    /**
     * 描述信息
     */
    String description() default "";
}
