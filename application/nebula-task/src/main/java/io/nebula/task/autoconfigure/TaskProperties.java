package io.nebula.task.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务配置属性
 */
@ConfigurationProperties(prefix = "nebula.task")
public class TaskProperties {
    
    /**
     * 是否启用任务功能
     */
    private boolean enabled = true;
    
    /**
     * 任务执行器配置
     */
    private Executor executor = new Executor();
    
    /**
     * XXL-JOB 配置
     */
    private XxlJob xxlJob = new XxlJob();
    
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Executor getExecutor() {
        return executor;
    }
    
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
    
    public XxlJob getXxlJob() {
        return xxlJob;
    }
    
    public void setXxlJob(XxlJob xxlJob) {
        this.xxlJob = xxlJob;
    }
    
    
    /**
     * 任务执行器配置
     */
    public static class Executor {
        
        /**
         * 线程池核心线程数
         */
        private int corePoolSize = 10;
        
        /**
         * 线程池最大线程数
         */
        private int maxPoolSize = 200;
        
        /**
         * 线程空闲时间（秒）
         */
        private int keepAliveSeconds = 60;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 1000;
        
        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "nebula-task-";
        
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }
        
        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }
    
    /**
     * XXL-JOB 配置
     */
    public static class XxlJob {
        
        /**
         * 是否启用 XXL-JOB
         */
        private boolean enabled = true;
        
        /**
         * 执行器名称
         */
        private String executorName;
        
        /**
         * 执行器IP
         */
        private String executorIp;
        
        /**
         * 执行器端口
         */
        private int executorPort = 9999;
        
        /**
         * 日志路径
         */
        private String logPath = "./logs/xxl-job";
        
        /**
         * 日志保留天数
         */
        private int logRetentionDays = 30;
        
        /**
         * 管理端地址
         */
        private String adminAddresses = "http://localhost:8080/xxl-job-admin";
        
    /**
     * 访问令牌
     */
    private String accessToken = "xxl-job";
    
    /**
     * 心跳注册间隔（秒）
     */
    private int heartbeatInterval = 30;
    
    /**
     * 注册重试次数
     */
    private int registryRetryCount = 3;
    
    /**
     * 注册超时时间（秒）
     */
    private int registryTimeout = 10;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getExecutorName() {
            return executorName;
        }
        
        public void setExecutorName(String executorName) {
            this.executorName = executorName;
        }
        
        public String getExecutorIp() {
            return executorIp;
        }
        
        public void setExecutorIp(String executorIp) {
            this.executorIp = executorIp;
        }
        
        public int getExecutorPort() {
            return executorPort;
        }
        
        public void setExecutorPort(int executorPort) {
            this.executorPort = executorPort;
        }
        
        public String getLogPath() {
            return logPath;
        }
        
        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }
        
        public int getLogRetentionDays() {
            return logRetentionDays;
        }
        
        public void setLogRetentionDays(int logRetentionDays) {
            this.logRetentionDays = logRetentionDays;
        }
        
        public String getAdminAddresses() {
            return adminAddresses;
        }
        
        public void setAdminAddresses(String adminAddresses) {
            this.adminAddresses = adminAddresses;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        public int getHeartbeatInterval() {
            return heartbeatInterval;
        }
        
        public void setHeartbeatInterval(int heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }
        
        public int getRegistryRetryCount() {
            return registryRetryCount;
        }
        
        public void setRegistryRetryCount(int registryRetryCount) {
            this.registryRetryCount = registryRetryCount;
        }
        
        public int getRegistryTimeout() {
            return registryTimeout;
        }
        
        public void setRegistryTimeout(int registryTimeout) {
            this.registryTimeout = registryTimeout;
        }
    }
    
}
