package io.nebula.core.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能监控注解
 * 标记需要进行性能监控的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    
    /**
     * 指标名称，如果为空则使用方法名
     * 
     * @return 指标名称
     */
    String value() default "";
    
    /**
     * 指标描述
     * 
     * @return 指标描述
     */
    String description() default "";
    
    /**
     * 是否记录执行时间
     * 
     * @return 是否记录执行时间
     */
    boolean recordTime() default true;
    
    /**
     * 是否记录执行次数
     * 
     * @return 是否记录执行次数
     */
    boolean recordCount() default true;
    
    /**
     * 是否记录错误
     * 
     * @return 是否记录错误
     */
    boolean recordErrors() default true;
    
    /**
     * 额外的标签
     * 格式：key1=value1,key2=value2
     * 
     * @return 额外的标签
     */
    String[] tags() default {};
    
    /**
     * 指标前缀
     * 
     * @return 指标前缀
     */
    String prefix() default "nebula";
}
