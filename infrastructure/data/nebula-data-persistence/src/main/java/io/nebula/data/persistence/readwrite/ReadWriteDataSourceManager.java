package io.nebula.data.persistence.readwrite;

import io.nebula.data.persistence.datasource.DataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 读写分离数据源管理器
 * 支持主从数据库的读写分离配置和路由
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "nebula.data.read-write-separation")
public class ReadWriteDataSourceManager {
    
    private final DataSourceManager dataSourceManager;
    private final Map<String, ReadWriteClusterConfig> clusters = new HashMap<>();
    
    // 负载均衡策略
    private final Map<String, AtomicInteger> roundRobinCounters = new HashMap<>();
    
    public ReadWriteDataSourceManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }
    
    /**
     * 获取写数据源（主库）
     */
    public DataSource getWriteDataSource() {
        return getWriteDataSource("default");
    }
    
    /**
     * 获取指定集群的写数据源
     */
    public DataSource getWriteDataSource(String clusterName) {
        ReadWriteClusterConfig cluster = clusters.get(clusterName);
        if (cluster == null) {
            log.warn("No read-write cluster config found for: {}, using default", clusterName);
            return dataSourceManager.getPrimaryDataSource();
        }
        
        String masterName = cluster.getMaster();
        if (masterName == null) {
            log.warn("No master configured for cluster: {}, using primary", clusterName);
            return dataSourceManager.getPrimaryDataSource();
        }
        
        DataSource masterDataSource = dataSourceManager.getDataSource(masterName);
        if (masterDataSource == null) {
            log.error("Master data source not found: {}, using primary", masterName);
            return dataSourceManager.getPrimaryDataSource();
        }
        
        log.debug("Using write data source: {} for cluster: {}", masterName, clusterName);
        return masterDataSource;
    }
    
    /**
     * 获取读数据源（从库）
     */
    public DataSource getReadDataSource() {
        return getReadDataSource("default");
    }
    
    /**
     * 获取指定集群的读数据源
     */
    public DataSource getReadDataSource(String clusterName) {
        ReadWriteClusterConfig cluster = clusters.get(clusterName);
        if (cluster == null) {
            log.warn("No read-write cluster config found for: {}, using write source", clusterName);
            return getWriteDataSource(clusterName);
        }
        
        List<String> slaves = cluster.getSlaves();
        if (slaves == null || slaves.isEmpty()) {
            log.debug("No slaves configured for cluster: {}, using master", clusterName);
            return getWriteDataSource(clusterName);
        }
        
        // 根据负载均衡策略选择从库
        String selectedSlave = selectSlave(clusterName, slaves, cluster.getLoadBalanceStrategy());
        DataSource slaveDataSource = dataSourceManager.getDataSource(selectedSlave);
        
        if (slaveDataSource == null) {
            log.warn("Selected slave data source not found: {}, using master", selectedSlave);
            return getWriteDataSource(clusterName);
        }
        
        log.debug("Using read data source: {} for cluster: {}", selectedSlave, clusterName);
        return slaveDataSource;
    }
    
    /**
     * 根据负载均衡策略选择从库
     */
    private String selectSlave(String clusterName, List<String> slaves, LoadBalanceStrategy strategy) {
        if (slaves.size() == 1) {
            return slaves.get(0);
        }
        
        switch (strategy) {
            case ROUND_ROBIN:
                return selectByRoundRobin(clusterName, slaves);
            case RANDOM:
                return selectByRandom(slaves);
            case WEIGHTED_ROUND_ROBIN:
                return selectByWeightedRoundRobin(clusterName, slaves);
            default:
                return selectByRoundRobin(clusterName, slaves);
        }
    }
    
    /**
     * 轮询策略
     */
    private String selectByRoundRobin(String clusterName, List<String> slaves) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(clusterName, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % slaves.size();
        return slaves.get(index);
    }
    
    /**
     * 随机策略
     */
    private String selectByRandom(List<String> slaves) {
        int index = ThreadLocalRandom.current().nextInt(slaves.size());
        return slaves.get(index);
    }
    
    /**
     * 加权轮询策略（简化实现）
     */
    private String selectByWeightedRoundRobin(String clusterName, List<String> slaves) {
        // 简化实现，实际应用中可以根据权重配置进行选择
        return selectByRoundRobin(clusterName, slaves);
    }
    
    /**
     * 检查集群是否启用读写分离
     */
    public boolean isReadWriteSeparationEnabled() {
        return isReadWriteSeparationEnabled("default");
    }
    
    /**
     * 检查指定集群是否启用读写分离
     */
    public boolean isReadWriteSeparationEnabled(String clusterName) {
        ReadWriteClusterConfig cluster = clusters.get(clusterName);
        return cluster != null && cluster.isEnabled();
    }
    
    /**
     * 获取所有集群名称
     */
    public Set<String> getClusterNames() {
        return new HashSet<>(clusters.keySet());
    }
    
    /**
     * 获取集群配置
     */
    public ReadWriteClusterConfig getClusterConfig(String clusterName) {
        return clusters.get(clusterName);
    }
    
    /**
     * 设置集群配置
     */
    public void setClusters(Map<String, ReadWriteClusterConfig> clusters) {
        this.clusters.clear();
        if (clusters != null) {
            this.clusters.putAll(clusters);
        }
        log.info("Read-write separation clusters configured: {}", this.clusters.keySet());
    }
    
    /**
     * 健康检查
     */
    public Map<String, Boolean> healthCheck() {
        Map<String, Boolean> healthStatus = new HashMap<>();
        
        for (Map.Entry<String, ReadWriteClusterConfig> entry : clusters.entrySet()) {
            String clusterName = entry.getKey();
            ReadWriteClusterConfig config = entry.getValue();
            
            try {
                // 检查主库
                boolean masterHealth = dataSourceManager.testConnection(config.getMaster());
                healthStatus.put(clusterName + ".master", masterHealth);
                
                // 检查从库
                if (config.getSlaves() != null) {
                    for (String slave : config.getSlaves()) {
                        boolean slaveHealth = dataSourceManager.testConnection(slave);
                        healthStatus.put(clusterName + ".slave." + slave, slaveHealth);
                    }
                }
            } catch (Exception e) {
                log.error("Health check failed for cluster: {}", clusterName, e);
                healthStatus.put(clusterName + ".error", false);
            }
        }
        
        return healthStatus;
    }
    
    /**
     * 获取集群统计信息
     */
    public Map<String, Object> getClusterStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (String clusterName : clusters.keySet()) {
            Map<String, Object> clusterStats = new HashMap<>();
            
            ReadWriteClusterConfig config = clusters.get(clusterName);
            clusterStats.put("enabled", config.isEnabled());
            clusterStats.put("master", config.getMaster());
            clusterStats.put("slaves", config.getSlaves());
            clusterStats.put("loadBalanceStrategy", config.getLoadBalanceStrategy());
            clusterStats.put("slaveCount", config.getSlaves() != null ? config.getSlaves().size() : 0);
            
            // 添加轮询计数器状态
            AtomicInteger counter = roundRobinCounters.get(clusterName);
            if (counter != null) {
                clusterStats.put("roundRobinCounter", counter.get());
            }
            
            stats.put(clusterName, clusterStats);
        }
        
        return stats;
    }
    
    /**
     * 读写分离集群配置
     */
    public static class ReadWriteClusterConfig {
        private boolean enabled = true;
        private String master;
        private List<String> slaves = new ArrayList<>();
        private LoadBalanceStrategy loadBalanceStrategy = LoadBalanceStrategy.ROUND_ROBIN;
        private boolean forceWriteOnMaster = true;
        private int maxRetries = 3;
        private long retryDelay = 1000; // milliseconds
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getMaster() { return master; }
        public void setMaster(String master) { this.master = master; }
        
        public List<String> getSlaves() { return slaves; }
        public void setSlaves(List<String> slaves) { this.slaves = slaves; }
        
        public LoadBalanceStrategy getLoadBalanceStrategy() { return loadBalanceStrategy; }
        public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) { 
            this.loadBalanceStrategy = loadBalanceStrategy; 
        }
        
        public boolean isForceWriteOnMaster() { return forceWriteOnMaster; }
        public void setForceWriteOnMaster(boolean forceWriteOnMaster) { 
            this.forceWriteOnMaster = forceWriteOnMaster; 
        }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public long getRetryDelay() { return retryDelay; }
        public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
    }
    
    /**
     * 负载均衡策略
     */
    public enum LoadBalanceStrategy {
        /**
         * 轮询
         */
        ROUND_ROBIN,
        
        /**
         * 随机
         */
        RANDOM,
        
        /**
         * 加权轮询
         */
        WEIGHTED_ROUND_ROBIN,
        
        /**
         * 最少连接
         */
        LEAST_CONNECTIONS
    }
}
