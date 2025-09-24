package io.nebula.discovery.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器工厂
 * 用于创建和管理不同类型的负载均衡器
 */
public class LoadBalancerFactory {
    
    private static final Map<LoadBalanceStrategy, LoadBalancer> LOAD_BALANCERS = new ConcurrentHashMap<>();
    
    static {
        // 初始化内置负载均衡器
        LOAD_BALANCERS.put(LoadBalanceStrategy.ROUND_ROBIN, new RoundRobinLoadBalancer());
        LOAD_BALANCERS.put(LoadBalanceStrategy.RANDOM, new RandomLoadBalancer());
        LOAD_BALANCERS.put(LoadBalanceStrategy.WEIGHTED_RANDOM, new WeightedRandomLoadBalancer());
    }
    
    /**
     * 获取负载均衡器
     * 
     * @param strategy 负载均衡策略
     * @return 负载均衡器实例
     */
    public static LoadBalancer getLoadBalancer(LoadBalanceStrategy strategy) {
        LoadBalancer loadBalancer = LOAD_BALANCERS.get(strategy);
        if (loadBalancer == null) {
            throw new IllegalArgumentException("Unsupported load balance strategy: " + strategy);
        }
        return loadBalancer;
    }
    
    /**
     * 获取默认负载均衡器（轮询）
     * 
     * @return 负载均衡器实例
     */
    public static LoadBalancer getDefaultLoadBalancer() {
        return getLoadBalancer(LoadBalanceStrategy.ROUND_ROBIN);
    }
    
    /**
     * 注册自定义负载均衡器
     * 
     * @param strategy 负载均衡策略
     * @param loadBalancer 负载均衡器实例
     */
    public static void registerLoadBalancer(LoadBalanceStrategy strategy, LoadBalancer loadBalancer) {
        LOAD_BALANCERS.put(strategy, loadBalancer);
    }
    
    /**
     * 创建负载均衡器
     * 
     * @param strategy 负载均衡策略
     * @return 负载均衡器实例
     */
    public static LoadBalancer createLoadBalancer(LoadBalanceStrategy strategy) {
        return switch (strategy) {
            case ROUND_ROBIN -> new RoundRobinLoadBalancer();
            case RANDOM -> new RandomLoadBalancer();
            case WEIGHTED_RANDOM -> new WeightedRandomLoadBalancer();
            case WEIGHTED_ROUND_ROBIN -> new WeightedRoundRobinLoadBalancer();
            case LEAST_ACTIVE -> new LeastActiveLoadBalancer();
            case CONSISTENT_HASH -> new ConsistentHashLoadBalancer();
            case FASTEST_RESPONSE -> new FastestResponseLoadBalancer();
            default -> throw new IllegalArgumentException("Unsupported load balance strategy: " + strategy);
        };
    }
    
    /**
     * 判断是否支持指定策略
     * 
     * @param strategy 负载均衡策略
     * @return 是否支持
     */
    public static boolean isSupported(LoadBalanceStrategy strategy) {
        return LOAD_BALANCERS.containsKey(strategy);
    }
    
    /**
     * 获取所有支持的策略
     * 
     * @return 支持的策略数组
     */
    public static LoadBalanceStrategy[] getSupportedStrategies() {
        return LOAD_BALANCERS.keySet().toArray(new LoadBalanceStrategy[0]);
    }
}
