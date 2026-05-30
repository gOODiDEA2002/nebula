package io.nebula.lock.redis;

import io.nebula.lock.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @Locked注解AOP切面处理器
 * 
 * 在方法执行前自动获取锁,执行后自动释放锁
 * 支持SpEL表达式动态生成锁key
 * 
 * 注：通过 RedisLockAutoConfiguration 自动配置，不使用 @Component
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LockedAspect {
    
    private final LockManager lockManager;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    /**
     * 拦截@Locked注解标记的方法
     */
    @Around("@annotation(io.nebula.lock.Locked)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Locked locked = resolveLockedAnnotation(joinPoint);

        // 解析锁key
        String lockKey = parseLockKey(locked.key(), joinPoint);
        
        log.debug("@Locked拦截: method={}, lockKey={}, waitTime={}s, leaseTime={}s",
                joinPoint.getSignature().toShortString(),
                lockKey,
                locked.waitTime(),
                locked.leaseTime());
        
        // 创建锁配置
        LockConfig config = LockConfig.builder()
                .waitTime(Duration.of(locked.waitTime(), toChronoUnit(locked.timeUnit())))
                .leaseTime(Duration.of(locked.leaseTime(), toChronoUnit(locked.timeUnit())))
                .lockType(locked.lockType())
                .enableWatchdog(locked.watchdog())
                .build();
        
        // 获取锁
        Lock lock = lockManager.getLock(lockKey, config);
        
        // 尝试获取锁
        boolean acquired = false;
        try {
            if (locked.waitTime() > 0) {
                // 等待获取锁
                acquired = lock.tryLock(locked.waitTime(), locked.timeUnit());
            } else {
                // 立即尝试获取锁
                acquired = lock.tryLock();
            }
            
            if (acquired) {
                log.debug("成功获取锁: lockKey={}, method={}",
                        lockKey, joinPoint.getSignature().toShortString());
                
                // 执行目标方法
                return joinPoint.proceed();
            } else {
                // 获取锁失败,根据失败策略处理
                return handleLockFailure(locked, joinPoint, lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: lockKey={}", lockKey, e);
            throw new LockAcquisitionException("Lock acquisition interrupted: " + lockKey, e);
        } finally {
            // 释放锁
            if (acquired) {
                try {
                    lock.unlock();
                    log.debug("释放锁: lockKey={}, method={}",
                            lockKey, joinPoint.getSignature().toShortString());
                } catch (Exception e) {
                    log.error("释放锁失败: lockKey={}", lockKey, e);
                }
            }
        }
    }

    private Locked resolveLockedAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Locked locked = AnnotationUtils.findAnnotation(method, Locked.class);
        Object target = joinPoint.getTarget();
        if (locked == null && target != null) {
            Method specificMethod = AopUtils.getMostSpecificMethod(method, target.getClass());
            locked = AnnotationUtils.findAnnotation(specificMethod, Locked.class);
        }

        if (locked == null) {
            throw new IllegalStateException("@Locked annotation not found on method: " + method);
        }
        return locked;
    }
    
    /**
     * 解析锁key
     * 支持SpEL表达式
     * 
     * @param keyExpression key表达式
     * @param joinPoint 切点
     * @return 解析后的锁key
     */
    private String parseLockKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        if (keyExpression == null || keyExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }
        
        // 如果不是SpEL表达式,直接返回
        if (!keyExpression.contains("#") && !keyExpression.contains("'")) {
            return keyExpression;
        }
        
        try {
            // 获取方法参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            
            // 创建SpEL上下文
            EvaluationContext context = new StandardEvaluationContext();
            
            // 将方法参数放入上下文
            if (parameterNames != null && args != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            
            // 解析表达式
            Expression expression = expressionParser.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            
            String lockKey = value != null ? value.toString() : keyExpression;
            log.debug("解析锁key: expression={}, result={}", keyExpression, lockKey);
            
            return lockKey;
        } catch (Exception e) {
            log.error("解析锁key失败: expression={}", keyExpression, e);
            throw new IllegalArgumentException("Failed to parse lock key: " + keyExpression, e);
        }
    }
    
    /**
     * 处理获取锁失败的情况
     */
    private Object handleLockFailure(Locked locked, ProceedingJoinPoint joinPoint, String lockKey) throws Throwable {
        String failMessage = parseFailMessage(locked.failMessage(), joinPoint, lockKey);
        logLockAcquisitionFailure(locked.failStrategy(), lockKey, joinPoint, failMessage);

        switch (locked.failStrategy()) {
            case THROW_EXCEPTION:
                throw new LockAcquisitionException(failMessage);

            case RETURN_NULL:
                return null;

            case RETURN_FALSE:
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                if (signature.getReturnType() == boolean.class || signature.getReturnType() == Boolean.class) {
                    return false;
                }
                log.warn("RETURN_FALSE策略只适用于boolean返回值,实际返回类型: {}",
                        signature.getReturnType().getName());
                return null;

            case SKIP:
                // 无锁执行：多实例下会导致重复执行，仅适合单实例或已确认无并发风险的场景
                return joinPoint.proceed();

            default:
                throw new LockAcquisitionException("Unknown fail strategy: " + locked.failStrategy());
        }
    }

    /**
     * 未抢到锁时的日志级别：
     * <ul>
     *   <li>RETURN_NULL / RETURN_FALSE：预期内的「本实例跳过」，降为 DEBUG，避免多副本定时任务刷屏</li>
     *   <li>SKIP / THROW_EXCEPTION：可能重复执行或抛错，保持 WARN</li>
     * </ul>
     */
    private void logLockAcquisitionFailure(
            Locked.FailStrategy strategy,
            String lockKey,
            ProceedingJoinPoint joinPoint,
            String failMessage) {
        String method = joinPoint.getSignature().toShortString();
        if (strategy == Locked.FailStrategy.RETURN_NULL || strategy == Locked.FailStrategy.RETURN_FALSE) {
            log.debug("获取锁失败(跳过执行): lockKey={}, method={}, strategy={}, message={}",
                    lockKey, method, strategy, failMessage);
            return;
        }
        if (strategy == Locked.FailStrategy.SKIP) {
            log.warn("获取锁失败(将无锁执行): lockKey={}, method={}, strategy={}, message={}",
                    lockKey, method, strategy, failMessage);
            return;
        }
        log.warn("获取锁失败: lockKey={}, method={}, strategy={}, message={}",
                lockKey, method, strategy, failMessage);
    }
    
    /**
     * 解析失败消息
     */
    private String parseFailMessage(String messageExpression, ProceedingJoinPoint joinPoint, String lockKey) {
        if (messageExpression == null || messageExpression.trim().isEmpty()) {
            return "Failed to acquire lock: " + lockKey;
        }
        
        // 如果不是SpEL表达式,直接返回
        if (!messageExpression.contains("#") && !messageExpression.contains("'")) {
            return messageExpression;
        }
        
        try {
            // 创建SpEL上下文
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("lockKey", lockKey);
            
            if (parameterNames != null && args != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            
            Expression expression = expressionParser.parseExpression(messageExpression);
            Object value = expression.getValue(context);
            
            return value != null ? value.toString() : messageExpression;
        } catch (Exception e) {
            log.error("解析失败消息失败: expression={}", messageExpression, e);
            return messageExpression;
        }
    }
    
    /**
     * 转换TimeUnit到ChronoUnit
     */
    private java.time.temporal.ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return java.time.temporal.ChronoUnit.NANOS;
            case MICROSECONDS:
                return java.time.temporal.ChronoUnit.MICROS;
            case MILLISECONDS:
                return java.time.temporal.ChronoUnit.MILLIS;
            case SECONDS:
                return java.time.temporal.ChronoUnit.SECONDS;
            case MINUTES:
                return java.time.temporal.ChronoUnit.MINUTES;
            case HOURS:
                return java.time.temporal.ChronoUnit.HOURS;
            case DAYS:
                return java.time.temporal.ChronoUnit.DAYS;
            default:
                return java.time.temporal.ChronoUnit.SECONDS;
        }
    }
}
