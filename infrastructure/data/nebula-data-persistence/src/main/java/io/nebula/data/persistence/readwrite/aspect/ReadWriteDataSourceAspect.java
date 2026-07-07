package io.nebula.data.persistence.readwrite.aspect;

import io.nebula.data.persistence.readwrite.DataSourceContextHolder;
import io.nebula.data.persistence.readwrite.DataSourceType;
import io.nebula.data.persistence.readwrite.annotation.ReadDataSource;
import io.nebula.data.persistence.readwrite.annotation.WriteDataSource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

import io.nebula.data.persistence.readwrite.ReadWriteDataSourceManager;

/**
 * 读写分离数据源切面
 * 根据注解自动切换数据源
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Aspect
@Order(1) // 确保在事务切面之前执行
public class ReadWriteDataSourceAspect {
    
    /**
     * 读数据源切点
     */
    @Pointcut("@annotation(io.nebula.data.persistence.readwrite.annotation.ReadDataSource) || " +
              "@within(io.nebula.data.persistence.readwrite.annotation.ReadDataSource)")
    public void readDataSourcePointcut() {}
    
    /**
     * 写数据源切点
     */
    @Pointcut("@annotation(io.nebula.data.persistence.readwrite.annotation.WriteDataSource) || " +
              "@within(io.nebula.data.persistence.readwrite.annotation.WriteDataSource)")
    public void writeDataSourcePointcut() {}
    
    /**
     * 读数据源切面处理
     */
    @Around("readDataSourcePointcut()")
    public Object aroundReadDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleDataSourceSwitch(joinPoint, DataSourceType.READ);
    }
    
    /**
     * 写数据源切面处理
     */
    @Around("writeDataSourcePointcut()")
    public Object aroundWriteDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleDataSourceSwitch(joinPoint, DataSourceType.WRITE);
    }
    
    /**
     * 处理数据源切换
     */
    private Object handleDataSourceSwitch(ProceedingJoinPoint joinPoint, DataSourceType targetType) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        // 获取注解信息
        DataSourceAnnotationInfo annotationInfo = getDataSourceAnnotationInfo(method, targetClass, targetType);
        
        // 检查是否需要切换数据源
        if (!shouldSwitchDataSource(annotationInfo, targetType, method, targetClass)) {
            log.debug("Skipping data source switch for method: {}", method.getName());
            return joinPoint.proceed();
        }
        
        // 保存当前数据源类型
        DataSourceType previousType = DataSourceContextHolder.getDataSourceType();
        
        try {
            // 设置新的数据源类型
            DataSourceContextHolder.setDataSourceType(targetType);
            
            log.debug("Switched to {} data source for method: {}.{}", 
                     targetType, targetClass.getSimpleName(), method.getName());
            
            // 执行目标方法
            return joinPoint.proceed();
            
        } finally {
            // 恢复之前的数据源类型
            if (previousType != null) {
                DataSourceContextHolder.setDataSourceType(previousType);
                log.debug("Restored data source type to: {}", previousType);
            } else {
                DataSourceContextHolder.clearDataSourceType();
                log.debug("Cleared data source type");
            }
        }
    }
    
    /**
     * 获取数据源注解信息
     */
    private DataSourceAnnotationInfo getDataSourceAnnotationInfo(Method method, Class<?> targetClass, DataSourceType targetType) {
        DataSourceAnnotationInfo info = new DataSourceAnnotationInfo();
        
        if (targetType == DataSourceType.READ) {
            // 先检查方法上的注解
            ReadDataSource methodAnnotation = AnnotationUtils.findAnnotation(method, ReadDataSource.class);
            if (methodAnnotation != null) {
                info.cluster = methodAnnotation.cluster();
                info.force = methodAnnotation.force();
                info.priority = methodAnnotation.priority();
                info.description = methodAnnotation.description();
                info.source = "method";
            } else {
                // 再检查类上的注解
                ReadDataSource classAnnotation = AnnotationUtils.findAnnotation(targetClass, ReadDataSource.class);
                if (classAnnotation != null) {
                    info.cluster = classAnnotation.cluster();
                    info.force = classAnnotation.force();
                    info.priority = classAnnotation.priority();
                    info.description = classAnnotation.description();
                    info.source = "class";
                }
            }
        } else {
            // 写数据源注解
            WriteDataSource methodAnnotation = AnnotationUtils.findAnnotation(method, WriteDataSource.class);
            if (methodAnnotation != null) {
                info.cluster = methodAnnotation.cluster();
                info.priority = methodAnnotation.priority();
                info.description = methodAnnotation.description();
                info.source = "method";
            } else {
                WriteDataSource classAnnotation = AnnotationUtils.findAnnotation(targetClass, WriteDataSource.class);
                if (classAnnotation != null) {
                    info.cluster = classAnnotation.cluster();
                    info.priority = classAnnotation.priority();
                    info.description = classAnnotation.description();
                    info.source = "class";
                }
            }
        }
        
        return info;
    }
    
    /**
     * 判断是否需要切换数据源
     */
    private boolean shouldSwitchDataSource(DataSourceAnnotationInfo annotationInfo, DataSourceType targetType,
                                           Method method, Class<?> targetClass) {
        if (targetType != DataSourceType.READ) {
            return true;
        }
        
        // CD-9: 本切面 @Order(1) 先于事务切面执行, 同方法 @ReadDataSource+@Transactional 时
        // isActualTransactionActive 恒为 false(事务尚未开启), 若此时切到读库, 随后开启的
        // "写事务"会绑定只读库连接, 写操作静默走从库/报错。故静态检测目标上声明的 @Transactional:
        // 非 readOnly 的写事务一律拒绝切读库(force 也不放行, 那会直接破坏写入)。
        Transactional declaredTx = findTransactional(method, targetClass);
        if (declaredTx != null && !declaredTx.readOnly()) {
            log.warn("Method {}.{} declares @Transactional(readOnly=false) with @ReadDataSource, " +
                            "refusing to switch to READ data source (writes would hit the read replica). " +
                            "Use @Transactional(readOnly=true) or remove @ReadDataSource.",
                    targetClass.getSimpleName(), method.getName());
            return false;
        }
        
        // 外层已有活动事务(事务方法内部再调读方法): 保持原有 force 语义
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            if (!annotationInfo.force) {
                log.debug("In transaction, skipping read data source switch (force=false)");
                return false;
            } else {
                log.warn("Forcing read data source in transaction, this may cause data consistency issues");
            }
        }
        
        return true;
    }
    
    /**
     * 查找目标方法/类上声明的 @Transactional(方法优先, 含接口与元注解)
     */
    private Transactional findTransactional(Method method, Class<?> targetClass) {
        Transactional tx = AnnotationUtils.findAnnotation(method, Transactional.class);
        if (tx == null) {
            tx = AnnotationUtils.findAnnotation(targetClass, Transactional.class);
        }
        return tx;
    }
    
    /**
     * 数据源注解信息
     */
    private static class DataSourceAnnotationInfo {
        String cluster = "default";
        boolean force = false;
        int priority = 0;
        String description = "";
        String source = "unknown";
    }
}
