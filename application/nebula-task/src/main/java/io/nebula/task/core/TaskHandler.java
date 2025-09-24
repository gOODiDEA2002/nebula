package io.nebula.task.core;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 任务处理器注解
 * 用于标记任务执行器，自动注册到任务引擎
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaskHandler {
    
    /**
     * 任务处理器名称
     * 如果不指定，则使用类名
     */
    String value() default "";
    
    /**
     * 任务处理器名称
     * 与 value 等价
     */
    String name() default "";
    
    /**
     * 描述信息
     */
    String description() default "";
    
    /**
     * 支持的任务类型
     */
    TaskType[] supportedTypes() default {TaskType.SCHEDULED, TaskType.MANUAL};
}
