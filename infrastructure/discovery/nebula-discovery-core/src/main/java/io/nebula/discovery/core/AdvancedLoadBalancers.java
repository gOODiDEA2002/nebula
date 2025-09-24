package io.nebula.discovery.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高级负载均衡器实现
 */

/**
 * 加权轮询负载均衡器
 */
class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    private final Map<String, AtomicInteger> currentWeights = new ConcurrentHashMap<>();
    
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
        
        if (availableInstances.size() == 1) {
            return availableInstances.get(0);
        }
        
        // 加权轮询算法
        ServiceInstance selectedInstance = null;
        int totalWeight = 0;
        
        for (ServiceInstance instance : availableInstances) {
            String key = instance.getUri();
            int weight = (int) Math.max(1, instance.getWeight());
            
            // 获取当前权重
            AtomicInteger currentWeight = currentWeights.computeIfAbsent(key, k -> new AtomicInteger(0));
            
            // 增加当前权重
            currentWeight.addAndGet(weight);
            totalWeight += weight;
            
            // 选择当前权重最大的实例
            if (selectedInstance == null || 
                currentWeight.get() > currentWeights.get(selectedInstance.getUri()).get()) {
                selectedInstance = instance;
            }
        }
        
        // 减少选中实例的当前权重
        if (selectedInstance != null) {
            currentWeights.get(selectedInstance.getUri()).addAndGet(-totalWeight);
        }
        
        return selectedInstance;
    }
}

/**
 * 最少活跃调用负载均衡器
 */
class LeastActiveLoadBalancer implements LoadBalancer {
    private final Map<String, AtomicLong> activeCounts = new ConcurrentHashMap<>();
    
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
        
        if (availableInstances.size() == 1) {
            return availableInstances.get(0);
        }
        
        // 找到活跃调用数最少的实例
        ServiceInstance leastActiveInstance = null;
        long leastActive = Long.MAX_VALUE;
        
        for (ServiceInstance instance : availableInstances) {
            String key = instance.getUri();
            long activeCount = activeCounts.computeIfAbsent(key, k -> new AtomicLong(0)).get();
            
            if (activeCount < leastActive) {
                leastActive = activeCount;
                leastActiveInstance = instance;
            } else if (activeCount == leastActive && leastActiveInstance != null) {
                // 如果活跃数相同，基于权重随机选择
                if (ThreadLocalRandom.current().nextDouble() * 
                    (instance.getWeight() + leastActiveInstance.getWeight()) < instance.getWeight()) {
                    leastActiveInstance = instance;
                }
            }
        }
        
        // 增加选中实例的活跃计数
        if (leastActiveInstance != null) {
            activeCounts.get(leastActiveInstance.getUri()).incrementAndGet();
        }
        
        return leastActiveInstance;
    }
    
    /**
     * 调用完成后减少活跃计数
     * 
     * @param instance 服务实例
     */
    public void completeCall(ServiceInstance instance) {
        if (instance != null) {
            activeCounts.computeIfAbsent(instance.getUri(), k -> new AtomicLong(0)).decrementAndGet();
        }
    }
}

/**
 * 一致性哈希负载均衡器
 */
class ConsistentHashLoadBalancer implements LoadBalancer {
    private final int virtualNodes = 160; // 虚拟节点数
    private final TreeMap<Long, ServiceInstance> hashRing = new TreeMap<>();
    private volatile List<ServiceInstance> lastInstances = new ArrayList<>();
    
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
        
        if (availableInstances.size() == 1) {
            return availableInstances.get(0);
        }
        
        // 如果实例列表发生变化，重建哈希环
        if (!availableInstances.equals(lastInstances)) {
            buildHashRing(availableInstances);
            lastInstances = new ArrayList<>(availableInstances);
        }
        
        return chooseFromHashRing();
    }
    
    @Override
    public ServiceInstance choose(List<ServiceInstance> instances, LoadBalanceContext context) {
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
        
        if (availableInstances.size() == 1) {
            return availableInstances.get(0);
        }
        
        // 如果实例列表发生变化，重建哈希环
        if (!availableInstances.equals(lastInstances)) {
            buildHashRing(availableInstances);
            lastInstances = new ArrayList<>(availableInstances);
        }
        
        return chooseFromHashRing(context);
    }
    
    private void buildHashRing(List<ServiceInstance> instances) {
        hashRing.clear();
        
        for (ServiceInstance instance : instances) {
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNodeKey = instance.getUri() + "#" + i;
                long hash = hash(virtualNodeKey);
                hashRing.put(hash, instance);
            }
        }
    }
    
    private ServiceInstance chooseFromHashRing() {
        // 使用随机键进行选择
        String randomKey = "random#" + ThreadLocalRandom.current().nextLong();
        return chooseFromHashRing(randomKey);
    }
    
    private ServiceInstance chooseFromHashRing(LoadBalanceContext context) {
        // 使用请求ID或用户ID作为哈希键
        String hashKey = context.getRequestId();
        if (hashKey == null || hashKey.isEmpty()) {
            hashKey = context.getUserId();
        }
        if (hashKey == null || hashKey.isEmpty()) {
            hashKey = "random#" + ThreadLocalRandom.current().nextLong();
        }
        
        return chooseFromHashRing(hashKey);
    }
    
    private ServiceInstance chooseFromHashRing(String key) {
        if (hashRing.isEmpty()) {
            return null;
        }
        
        long hash = hash(key);
        
        // 找到第一个大于等于该哈希值的节点
        Map.Entry<Long, ServiceInstance> entry = hashRing.ceilingEntry(hash);
        
        // 如果没有找到，选择第一个节点（形成环）
        if (entry == null) {
            entry = hashRing.firstEntry();
        }
        
        return entry.getValue();
    }
    
    private long hash(String key) {
        // 简单的哈希函数，实际项目中可以使用更好的哈希算法如MurmurHash
        return key.hashCode() & 0x7fffffffL;
    }
}

/**
 * 最快响应负载均衡器
 */
class FastestResponseLoadBalancer implements LoadBalancer {
    private final Map<String, AtomicLong> responseTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> callCounts = new ConcurrentHashMap<>();
    
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
        
        if (availableInstances.size() == 1) {
            return availableInstances.get(0);
        }
        
        // 找到平均响应时间最短的实例
        ServiceInstance fastestInstance = null;
        long shortestAvgResponseTime = Long.MAX_VALUE;
        
        for (ServiceInstance instance : availableInstances) {
            String key = instance.getUri();
            long totalResponseTime = responseTimes.computeIfAbsent(key, k -> new AtomicLong(0)).get();
            long totalCalls = callCounts.computeIfAbsent(key, k -> new AtomicLong(0)).get();
            
            long avgResponseTime;
            if (totalCalls == 0) {
                // 如果没有调用历史，给个默认值，优先选择
                avgResponseTime = 1;
            } else {
                avgResponseTime = totalResponseTime / totalCalls;
            }
            
            if (avgResponseTime < shortestAvgResponseTime) {
                shortestAvgResponseTime = avgResponseTime;
                fastestInstance = instance;
            } else if (avgResponseTime == shortestAvgResponseTime && fastestInstance != null) {
                // 如果响应时间相同，基于权重随机选择
                if (ThreadLocalRandom.current().nextDouble() * 
                    (instance.getWeight() + fastestInstance.getWeight()) < instance.getWeight()) {
                    fastestInstance = instance;
                }
            }
        }
        
        return fastestInstance;
    }
    
    /**
     * 记录调用响应时间
     * 
     * @param instance 服务实例
     * @param responseTime 响应时间（毫秒）
     */
    public void recordResponseTime(ServiceInstance instance, long responseTime) {
        if (instance != null && responseTime > 0) {
            String key = instance.getUri();
            responseTimes.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(responseTime);
            callCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    /**
     * 获取平均响应时间
     * 
     * @param instance 服务实例
     * @return 平均响应时间，没有数据返回-1
     */
    public long getAverageResponseTime(ServiceInstance instance) {
        if (instance == null) {
            return -1;
        }
        
        String key = instance.getUri();
        long totalResponseTime = responseTimes.getOrDefault(key, new AtomicLong(0)).get();
        long totalCalls = callCounts.getOrDefault(key, new AtomicLong(0)).get();
        
        return totalCalls > 0 ? totalResponseTime / totalCalls : -1;
    }
}
