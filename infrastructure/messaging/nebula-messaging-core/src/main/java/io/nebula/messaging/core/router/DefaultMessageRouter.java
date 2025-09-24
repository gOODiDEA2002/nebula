package io.nebula.messaging.core.router;

import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认消息路由器实现
 * 支持多种路由策略和规则匹配
 */
@Slf4j
public class DefaultMessageRouter implements MessageRouter {
    
    private final Map<String, String> routes = new ConcurrentHashMap<>();
    private final Map<RouteCondition, String> conditionRoutes = new ConcurrentHashMap<>();
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    private String defaultRoute = "default.queue";
    private RouteStrategy strategy = RouteStrategy.TOPIC_BASED;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    public DefaultMessageRouter() {
        log.info("默认消息路由器初始化完成，策略: {}", strategy);
    }
    
    public DefaultMessageRouter(RouteStrategy strategy) {
        this.strategy = strategy;
        log.info("默认消息路由器初始化完成，策略: {}", strategy);
    }
    
    @Override
    public String route(Message<?> message) {
        if (message == null) {
            log.warn("消息为空，使用默认路由: {}", defaultRoute);
            return defaultRoute;
        }
        
        try {
            String destination = routeByStrategy(message);
            
            if (destination == null || destination.isEmpty()) {
                log.debug("未找到匹配的路由规则，使用默认路由: {} for message: {}", 
                        defaultRoute, message.getId());
                destination = defaultRoute;
            }
            
            log.debug("消息路由完成: messageId={}, topic={}, destination={}", 
                    message.getId(), message.getTopic(), destination);
            
            return destination;
        } catch (Exception e) {
            log.error("消息路由异常，使用默认路由: messageId={}, error={}", 
                    message.getId(), e.getMessage(), e);
            return defaultRoute;
        }
    }
    
    @Override
    public Map<String, List<Message<?>>> routeBatch(List<Message<?>> messages) {
        if (messages == null || messages.isEmpty()) {
            return new HashMap<>();
        }
        
        return messages.stream()
                .collect(Collectors.groupingBy(this::route));
    }
    
    @Override
    public void addRoute(String pattern, String destination) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("路由模式不能为空");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("目标队列不能为空");
        }
        
        routes.put(pattern.trim(), destination.trim());
        
        // 预编译正则表达式
        try {
            String regexPattern = convertToRegex(pattern.trim());
            patternCache.put(pattern.trim(), Pattern.compile(regexPattern));
        } catch (Exception e) {
            log.warn("路由模式编译失败，将使用字符串匹配: pattern={}, error={}", 
                    pattern, e.getMessage());
        }
        
        log.info("添加路由规则: {} -> {}", pattern, destination);
    }
    
    @Override
    public void addRoute(RouteCondition condition, String destination) {
        if (condition == null) {
            throw new IllegalArgumentException("路由条件不能为空");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("目标队列不能为空");
        }
        
        conditionRoutes.put(condition, destination.trim());
        log.info("添加条件路由规则: {} -> {}", condition.getClass().getSimpleName(), destination);
    }
    
    @Override
    public void removeRoute(String pattern) {
        if (pattern != null) {
            routes.remove(pattern.trim());
            patternCache.remove(pattern.trim());
            log.info("移除路由规则: {}", pattern);
        }
    }
    
    @Override
    public Map<String, String> getRoutes() {
        return new HashMap<>(routes);
    }
    
    @Override
    public List<String> getPossibleRoutes(Message<?> message) {
        List<String> possibleRoutes = new ArrayList<>();
        
        // 检查模式路由
        for (Map.Entry<String, String> entry : routes.entrySet()) {
            if (matchesPattern(message, entry.getKey())) {
                possibleRoutes.add(entry.getValue());
            }
        }
        
        // 检查条件路由
        for (Map.Entry<RouteCondition, String> entry : conditionRoutes.entrySet()) {
            try {
                if (entry.getKey().matches(message)) {
                    possibleRoutes.add(entry.getValue());
                }
            } catch (Exception e) {
                log.warn("条件路由检查异常: {}", e.getMessage());
            }
        }
        
        // 如果没有匹配的路由，添加默认路由
        if (possibleRoutes.isEmpty()) {
            possibleRoutes.add(defaultRoute);
        }
        
        return possibleRoutes;
    }
    
    @Override
    public boolean hasRoute(String pattern) {
        return pattern != null && routes.containsKey(pattern.trim());
    }
    
    @Override
    public void clearRoutes() {
        routes.clear();
        conditionRoutes.clear();
        patternCache.clear();
        log.info("清空所有路由规则");
    }
    
    @Override
    public void setDefaultRoute(String defaultDestination) {
        if (defaultDestination == null || defaultDestination.trim().isEmpty()) {
            throw new IllegalArgumentException("默认路由不能为空");
        }
        this.defaultRoute = defaultDestination.trim();
        log.info("设置默认路由: {}", this.defaultRoute);
    }
    
    @Override
    public String getDefaultRoute() {
        return defaultRoute;
    }
    
    /**
     * 设置路由策略
     * 
     * @param strategy 路由策略
     */
    public void setStrategy(RouteStrategy strategy) {
        if (strategy != null) {
            this.strategy = strategy;
            log.info("更新路由策略: {}", strategy);
        }
    }
    
    /**
     * 获取路由策略
     * 
     * @return 路由策略
     */
    public RouteStrategy getStrategy() {
        return strategy;
    }
    
    /**
     * 根据策略进行路由
     */
    private String routeByStrategy(Message<?> message) {
        return switch (strategy) {
            case TOPIC_BASED -> routeByTopic(message);
            case TAG_BASED -> routeByTag(message);
            case HEADER_BASED -> routeByHeader(message);
            case CONTENT_BASED -> routeByContent(message);
            case CUSTOM_CONDITION -> routeByCondition(message);
            case ROUND_ROBIN -> routeByRoundRobin(message);
            case RANDOM -> routeByRandom(message);
            case CONSISTENT_HASH -> routeByConsistentHash(message);
            default -> routeByTopic(message);
        };
    }
    
    /**
     * 基于主题路由
     */
    private String routeByTopic(Message<?> message) {
        String topic = message.getTopic();
        if (topic == null) {
            return null;
        }
        
        // 首先尝试精确匹配
        String exactMatch = routes.get(topic);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // 然后尝试模式匹配
        for (Map.Entry<String, String> entry : routes.entrySet()) {
            if (matchesPattern(message, entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 基于标签路由
     */
    private String routeByTag(Message<?> message) {
        String tag = message.getTag();
        if (tag == null) {
            return routeByTopic(message);  // 回退到主题路由
        }
        
        return routes.get(tag);
    }
    
    /**
     * 基于消息头路由
     */
    private String routeByHeader(Message<?> message) {
        Map<String, String> headers = message.getHeaders();
        if (headers == null || headers.isEmpty()) {
            return routeByTopic(message);  // 回退到主题路由
        }
        
        // 检查路由头
        String routeHeader = headers.get("route");
        if (routeHeader != null) {
            return routes.get(routeHeader);
        }
        
        return routeByTopic(message);  // 回退到主题路由
    }
    
    /**
     * 基于内容路由
     */
    private String routeByContent(Message<?> message) {
        // 这里可以根据消息载荷内容进行路由
        // 简化实现：基于载荷类型
        if (message.getPayload() != null) {
            String className = message.getPayload().getClass().getSimpleName();
            String destination = routes.get(className);
            if (destination != null) {
                return destination;
            }
        }
        
        return routeByTopic(message);  // 回退到主题路由
    }
    
    /**
     * 基于条件路由
     */
    private String routeByCondition(Message<?> message) {
        for (Map.Entry<RouteCondition, String> entry : conditionRoutes.entrySet()) {
            try {
                if (entry.getKey().matches(message)) {
                    return entry.getValue();
                }
            } catch (Exception e) {
                log.warn("条件路由检查异常: {}", e.getMessage());
            }
        }
        
        return routeByTopic(message);  // 回退到主题路由
    }
    
    /**
     * 轮询路由
     */
    private String routeByRoundRobin(Message<?> message) {
        List<String> destinations = new ArrayList<>(routes.values());
        if (destinations.isEmpty()) {
            return null;
        }
        
        int index = roundRobinCounter.getAndIncrement() % destinations.size();
        return destinations.get(index);
    }
    
    /**
     * 随机路由
     */
    private String routeByRandom(Message<?> message) {
        List<String> destinations = new ArrayList<>(routes.values());
        if (destinations.isEmpty()) {
            return null;
        }
        
        int index = new Random().nextInt(destinations.size());
        return destinations.get(index);
    }
    
    /**
     * 一致性哈希路由
     */
    private String routeByConsistentHash(Message<?> message) {
        List<String> destinations = new ArrayList<>(routes.values());
        if (destinations.isEmpty()) {
            return null;
        }
        
        // 使用消息ID进行哈希
        String hashKey = message.getId() != null ? message.getId() : message.getTopic();
        if (hashKey == null) {
            hashKey = String.valueOf(System.currentTimeMillis());
        }
        
        int hash = Math.abs(hashKey.hashCode());
        int index = hash % destinations.size();
        return destinations.get(index);
    }
    
    /**
     * 检查消息是否匹配模式
     */
    private boolean matchesPattern(Message<?> message, String pattern) {
        String topic = message.getTopic();
        if (topic == null) {
            return false;
        }
        
        // 尝试使用预编译的正则表达式
        Pattern compiledPattern = patternCache.get(pattern);
        if (compiledPattern != null) {
            return compiledPattern.matcher(topic).matches();
        }
        
        // 简单的通配符匹配
        return simpleWildcardMatch(topic, pattern);
    }
    
    /**
     * 将通配符模式转换为正则表达式
     */
    private String convertToRegex(String wildcardPattern) {
        // 替换通配符为正则表达式
        String regex = wildcardPattern
                .replace(".", "\\.")  // 转义点号
                .replace("*", "[^.]*")  // * 匹配除点号外的任意字符
                .replace("#", ".*");    // # 匹配任意字符
        
        return "^" + regex + "$";
    }
    
    /**
     * 简单的通配符匹配
     */
    private boolean simpleWildcardMatch(String text, String pattern) {
        // 简化的通配符实现
        if (pattern.equals("*") || pattern.equals("#")) {
            return true;
        }
        
        if (pattern.contains("*") || pattern.contains("#")) {
            // 简单实现：只支持后缀匹配
            if (pattern.endsWith("*") || pattern.endsWith("#")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                return text.startsWith(prefix);
            }
        }
        
        return text.equals(pattern);
    }
}
