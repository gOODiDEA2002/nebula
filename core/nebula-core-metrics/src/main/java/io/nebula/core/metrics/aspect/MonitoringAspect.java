package io.nebula.core.metrics.aspect;

import io.micrometer.core.instrument.Timer;
import io.nebula.core.metrics.MetricsCollector;
import io.nebula.core.metrics.annotation.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能监控切面
 * 自动为标记了@Monitored注解的方法添加性能监控
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MonitoringAspect {
    
    private final MetricsCollector metricsCollector;
    
    /**
     * 环绕通知，监控方法执行
     * 
     * @param joinPoint 切点
     * @param monitored 监控注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(monitored)")
    public Object monitor(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String operationName = getOperationName(joinPoint, monitored);
        String[] tags = buildTags(joinPoint, monitored);
        
        Timer.Sample sample = null;
        if (monitored.recordTime()) {
            sample = metricsCollector.startTimer();
        }
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            
            // 记录成功次数
            if (monitored.recordCount()) {
                metricsCollector.increment(buildMetricName(monitored.prefix(), operationName, "success"), tags);
                metricsCollector.increment(buildMetricName(monitored.prefix(), operationName, "total"), tags);
            }
            
            return result;
            
        } catch (Throwable e) {
            exception = e;
            
            // 记录错误次数和类型
            if (monitored.recordErrors()) {
                String[] errorTags = addErrorTags(tags, e);
                metricsCollector.increment(buildMetricName(monitored.prefix(), operationName, "error"), errorTags);
                metricsCollector.increment(buildMetricName(monitored.prefix(), operationName, "total"), tags);
            }
            
            throw e;
            
        } finally {
            // 记录执行时间
            if (monitored.recordTime() && sample != null) {
                metricsCollector.stopTimer(sample, buildMetricName(monitored.prefix(), operationName, "duration"), tags);
            }
            
            // 记录执行时间（毫秒）
            long duration = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
                log.debug("Method {} executed in {}ms, success={}", 
                        operationName, duration, exception == null);
            }
        }
    }
    
    /**
     * 获取操作名称
     * 
     * @param joinPoint 切点
     * @param monitored 监控注解
     * @return 操作名称
     */
    private String getOperationName(ProceedingJoinPoint joinPoint, Monitored monitored) {
        if (!monitored.value().isEmpty()) {
            return monitored.value();
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        return className + "." + methodName;
    }
    
    /**
     * 构建标签
     * 
     * @param joinPoint 切点
     * @param monitored 监控注解
     * @return 标签数组
     */
    private String[] buildTags(ProceedingJoinPoint joinPoint, Monitored monitored) {
        List<String> tagList = new ArrayList<>();
        
        // 添加类名和方法名标签
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        tagList.add("class");
        tagList.add(signature.getDeclaringType().getSimpleName());
        tagList.add("method");
        tagList.add(signature.getName());
        
        // 添加自定义标签
        for (String tag : monitored.tags()) {
            if (tag.contains("=")) {
                String[] parts = tag.split("=", 2);
                if (parts.length == 2) {
                    tagList.add(parts[0].trim());
                    tagList.add(parts[1].trim());
                }
            }
        }
        
        return tagList.toArray(new String[0]);
    }
    
    /**
     * 添加错误标签
     * 
     * @param originalTags 原始标签
     * @param exception    异常
     * @return 包含错误信息的标签数组
     */
    private String[] addErrorTags(String[] originalTags, Throwable exception) {
        List<String> tagList = new ArrayList<>();
        
        // 添加原始标签
        for (String tag : originalTags) {
            tagList.add(tag);
        }
        
        // 添加错误类型标签
        tagList.add("error.type");
        tagList.add(exception.getClass().getSimpleName());
        
        // 添加错误消息标签（截取前100个字符）
        String message = exception.getMessage();
        if (message != null) {
            tagList.add("error.message");
            tagList.add(message.length() > 100 ? message.substring(0, 100) + "..." : message);
        }
        
        return tagList.toArray(new String[0]);
    }
    
    /**
     * 构建指标名称
     * 
     * @param prefix        前缀
     * @param operationName 操作名称
     * @param suffix        后缀
     * @return 指标名称
     */
    private String buildMetricName(String prefix, String operationName, String suffix) {
        return String.format("%s.%s.%s", prefix, operationName.toLowerCase().replace(".", "_"), suffix);
    }
}
