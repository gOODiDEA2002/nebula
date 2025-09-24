package io.nebula.task.xxljob.service;

import io.nebula.task.autoconfigure.TaskProperties;
import io.nebula.task.xxljob.dto.XxlJobResult;
import io.nebula.task.xxljob.util.XxlJobHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * XXL-JOB 执行器注册服务
 * 负责向 XXL-JOB 管理端注册和注销执行器
 */
@Service
@Slf4j
public class XxlJobRegistryService {
    
    private static final String REGISTRY_GROUP = "EXECUTOR";
    
    @Autowired(required = false)
    private TaskProperties taskProperties;
    
    @Autowired(required = false)
    private XxlJobHttpClient httpClient;
    
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean started = false;
    private final AtomicBoolean registrySuccess = new AtomicBoolean(false);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    
    /**
     * 启动注册服务
     */
    public void start() {
        if (started) {
            return;
        }
        
        if (taskProperties == null || httpClient == null) {
            log.info("XXL-JOB 配置或HTTP客户端不可用，跳过执行器注册");
            return;
        }
        
        if (!taskProperties.getXxlJob().isEnabled()) {
            log.info("XXL-JOB 未启用，跳过执行器注册");
            return;
        }
        
        // 立即注册一次
        registryWithRetry();
        
        // 定期注册心跳（使用配置的间隔）
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "xxl-job-registry");
            thread.setDaemon(true);
            return thread;
        });
        
        int heartbeatInterval = taskProperties.getXxlJob().getHeartbeatInterval();
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                registryWithRetry();
            } catch (Exception e) {
                log.error("执行器注册任务异常", e);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
        
        started = true;
        log.info("XXL-JOB 执行器注册服务已启动");
    }
    
    /**
     * 停止注册服务
     */
    public void stop() {
        if (!started) {
            return;
        }
        
        // 注销执行器
        registryRemove();
        
        // 停止定时任务
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        started = false;
        log.info("XXL-JOB 执行器注册服务已停止");
    }
    
    /**
     * 带重试的注册执行器
     */
    private void registryWithRetry() {
        int retryCount = taskProperties.getXxlJob().getRegistryRetryCount();
        boolean success = false;
        
        for (int i = 0; i <= retryCount && !success; i++) {
            try {
                success = registry();
                if (success) {
                    // 注册成功，重置失败计数
                    if (failureCount.get() > 0) {
                        log.info("执行器注册恢复成功: {}", getExecutorName());
                        failureCount.set(0);
                    }
                    registrySuccess.set(true);
                } else if (i < retryCount) {
                    // 注册失败但还有重试次数
                    log.warn("执行器注册失败，第 {} 次重试: {}", i + 1, getExecutorName());
                    Thread.sleep(2000); // 重试间隔2秒
                }
            } catch (Exception e) {
                if (i < retryCount) {
                    log.warn("执行器注册异常，第 {} 次重试: {}", i + 1, getExecutorName(), e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("执行器注册最终失败: {}", getExecutorName(), e);
                }
            }
        }
        
        if (!success) {
            int failures = failureCount.incrementAndGet();
            registrySuccess.set(false);
            if (failures % 10 == 1) { // 每10次失败记录一次错误日志
                log.error("执行器注册持续失败 {} 次: {}", failures, getExecutorName());
            }
        }
    }
    
    /**
     * 注册执行器
     * @return 是否成功
     */
    private boolean registry() {
        String registryUrl = getRegistryUrl();
        if (registryUrl == null) {
            return false;
        }
        
        Map<String, Object> registryRequest = new HashMap<>();
        registryRequest.put("registryGroup", REGISTRY_GROUP);
        registryRequest.put("registryKey", getExecutorName());
        registryRequest.put("registryValue", getExecutorAddress());
        
        XxlJobResult result = httpClient.post(
                registryUrl + "/api/registry", 
                registryRequest, 
                taskProperties.getXxlJob().getAccessToken(), 
                XxlJobResult.class
        );
        
        if (result != null && result.isSuccess()) {
            // 只在状态变化时或首次成功时记录info日志
            if (!registrySuccess.get() || failureCount.get() > 0) {
                log.info("执行器注册成功: {}", getExecutorName());
            } else {
                log.debug("执行器心跳注册: {}", getExecutorName());
            }
            return true;
        } else {
            log.warn("执行器注册失败: {}, result={}", getExecutorName(), result);
            return false;
        }
    }
    
    /**
     * 注销执行器
     */
    private void registryRemove() {
        String registryUrl = getRegistryUrl();
        if (registryUrl == null) {
            return;
        }
        
        Map<String, Object> registryRequest = new HashMap<>();
        registryRequest.put("registryGroup", REGISTRY_GROUP);
        registryRequest.put("registryKey", getExecutorName());
        registryRequest.put("registryValue", getExecutorAddress());
        
        try {
            XxlJobResult result = httpClient.post(
                    registryUrl + "/api/registryRemove", 
                    registryRequest, 
                    taskProperties.getXxlJob().getAccessToken(), 
                    XxlJobResult.class
            );
            
            if (result != null && result.isSuccess()) {
                log.info("执行器注销成功: {}", getExecutorName());
            } else {
                log.warn("执行器注销失败: {}, result={}", getExecutorName(), result);
            }
            
        } catch (Exception e) {
            log.error("执行器注销异常: {}", getExecutorName(), e);
        }
    }
    
    /**
     * 获取注册地址
     */
    private String getRegistryUrl() {
        String addresses = taskProperties.getXxlJob().getAdminAddresses();
        if (addresses == null || addresses.trim().isEmpty()) {
            log.warn("XXL-JOB 管理端地址未配置");
            return null;
        }
        
        // 取第一个地址
        if (addresses.contains(",")) {
            addresses = addresses.split(",")[0].trim();
        }
        
        return addresses;
    }
    
    /**
     * 获取执行器名称
     */
    private String getExecutorName() {
        String name = taskProperties.getXxlJob().getExecutorName();
        if (name == null || name.trim().isEmpty()) {
            name = "nebula-task-executor";
        }
        return name;
    }
    
    /**
     * 获取执行器地址
     */
    private String getExecutorAddress() {
        String ip = taskProperties.getXxlJob().getExecutorIp();
        if (ip == null || ip.trim().isEmpty()) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.warn("获取本机IP失败，使用默认IP", e);
                ip = "127.0.0.1";
            }
        }
        
        int port = taskProperties.getXxlJob().getExecutorPort();
        return String.format("http://%s:%d", ip, port);
    }
    
    /**
     * 获取注册状态
     * 
     * @return 是否已成功注册
     */
    public boolean isRegistrySuccess() {
        return registrySuccess.get();
    }
    
    /**
     * 获取连续失败次数
     * 
     * @return 失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * 获取注册状态信息
     * 
     * @return 状态信息
     */
    public Map<String, Object> getRegistryStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("started", started);
        status.put("registrySuccess", registrySuccess.get());
        status.put("failureCount", failureCount.get());
        status.put("executorName", getExecutorName());
        status.put("executorAddress", getExecutorAddress());
        status.put("heartbeatInterval", taskProperties.getXxlJob().getHeartbeatInterval());
        return status;
    }
}
