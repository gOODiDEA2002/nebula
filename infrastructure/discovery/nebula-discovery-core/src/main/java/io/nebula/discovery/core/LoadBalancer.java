package io.nebula.discovery.core;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡器接口
 * 用于在多个服务实例中选择一个进行调用
 */
public interface LoadBalancer {
    
    /**
     * 从服务实例列表中选择一个实例
     * 
     * @param instances 服务实例列表
     * @return 选中的服务实例，如果没有可用实例返回null
     */
    ServiceInstance choose(List<ServiceInstance> instances);
    
    /**
     * 从服务实例列表中选择一个实例（带上下文）
     * 
     * @param instances 服务实例列表
     * @param context 选择上下文（可包含请求信息、用户信息等）
     * @return 选中的服务实例，如果没有可用实例返回null
     */
    default ServiceInstance choose(List<ServiceInstance> instances, LoadBalanceContext context) {
        return choose(instances);
    }
}



/**
 * 负载均衡上下文
 * 包含选择实例时需要的上下文信息
 */
class LoadBalanceContext {
    private final String requestId;
    private final String userId;
    private final String clientIp;
    private final Object request;
    
    public LoadBalanceContext(String requestId, String userId, String clientIp, Object request) {
        this.requestId = requestId;
        this.userId = userId;
        this.clientIp = clientIp;
        this.request = request;
    }
    
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getClientIp() { return clientIp; }
    public Object getRequest() { return request; }
}

/**
 * 轮询负载均衡器实现
 */
class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // 过滤出可用实例
        List<ServiceInstance> availableInstances = instances.stream()
                .filter(ServiceInstance::isAvailable)
                .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        int index = Math.abs(counter.getAndIncrement()) % availableInstances.size();
        return availableInstances.get(index);
    }
}

/**
 * 随机负载均衡器实现
 */
class RandomLoadBalancer implements LoadBalancer {
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // 过滤出可用实例
        List<ServiceInstance> availableInstances = instances.stream()
                .filter(ServiceInstance::isAvailable)
                .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        int index = ThreadLocalRandom.current().nextInt(availableInstances.size());
        return availableInstances.get(index);
    }
}

/**
 * 加权随机负载均衡器实现
 */
class WeightedRandomLoadBalancer implements LoadBalancer {
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // 过滤出可用实例
        List<ServiceInstance> availableInstances = instances.stream()
                .filter(ServiceInstance::isAvailable)
                .toList();
        
        if (availableInstances.isEmpty()) {
            return null;
        }
        
        // 计算总权重
        double totalWeight = availableInstances.stream()
                .mapToDouble(ServiceInstance::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            // 如果没有权重信息，使用随机选择
            int index = ThreadLocalRandom.current().nextInt(availableInstances.size());
            return availableInstances.get(index);
        }
        
        // 生成随机数
        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        
        // 选择实例
        double currentWeight = 0;
        for (ServiceInstance instance : availableInstances) {
            currentWeight += instance.getWeight();
            if (random <= currentWeight) {
                return instance;
            }
        }
        
        // 兜底返回最后一个实例
        return availableInstances.get(availableInstances.size() - 1);
    }
}
