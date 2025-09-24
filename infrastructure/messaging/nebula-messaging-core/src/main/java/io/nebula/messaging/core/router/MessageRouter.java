package io.nebula.messaging.core.router;

import io.nebula.messaging.core.message.Message;

import java.util.List;
import java.util.Map;

/**
 * 消息路由器接口
 * 负责根据消息内容和路由规则确定消息的目标队列
 */
public interface MessageRouter {
    
    /**
     * 路由消息，返回目标队列名称
     * 
     * @param message 消息对象
     * @return 目标队列名称
     */
    String route(Message<?> message);
    
    /**
     * 批量路由消息
     * 
     * @param messages 消息列表
     * @return 消息到队列的映射
     */
    Map<String, List<Message<?>>> routeBatch(List<Message<?>> messages);
    
    /**
     * 添加路由规则
     * 
     * @param pattern 匹配模式 (支持通配符，如 user.*, order.#)
     * @param destination 目标队列
     */
    void addRoute(String pattern, String destination);
    
    /**
     * 添加条件路由规则
     * 
     * @param condition 路由条件
     * @param destination 目标队列
     */
    void addRoute(RouteCondition condition, String destination);
    
    /**
     * 移除路由规则
     * 
     * @param pattern 匹配模式
     */
    void removeRoute(String pattern);
    
    /**
     * 获取所有路由规则
     * 
     * @return 路由规则映射 (模式 -> 目标队列)
     */
    Map<String, String> getRoutes();
    
    /**
     * 获取指定消息的所有可能路由
     * 
     * @param message 消息对象
     * @return 可能的目标队列列表
     */
    List<String> getPossibleRoutes(Message<?> message);
    
    /**
     * 检查路由规则是否存在
     * 
     * @param pattern 匹配模式
     * @return 是否存在
     */
    boolean hasRoute(String pattern);
    
    /**
     * 清空所有路由规则
     */
    void clearRoutes();
    
    /**
     * 设置默认路由
     * 
     * @param defaultDestination 默认目标队列
     */
    void setDefaultRoute(String defaultDestination);
    
    /**
     * 获取默认路由
     * 
     * @return 默认目标队列
     */
    String getDefaultRoute();
    
    /**
     * 路由条件接口
     */
    @FunctionalInterface
    interface RouteCondition {
        /**
         * 判断消息是否满足路由条件
         * 
         * @param message 消息对象
         * @return 是否满足条件
         */
        boolean matches(Message<?> message);
    }
    
    /**
     * 路由策略枚举
     */
    enum RouteStrategy {
        /**
         * 基于主题路由
         */
        TOPIC_BASED,
        
        /**
         * 基于标签路由
         */
        TAG_BASED,
        
        /**
         * 基于消息头路由
         */
        HEADER_BASED,
        
        /**
         * 基于载荷内容路由
         */
        CONTENT_BASED,
        
        /**
         * 自定义条件路由
         */
        CUSTOM_CONDITION,
        
        /**
         * 轮询路由
         */
        ROUND_ROBIN,
        
        /**
         * 随机路由
         */
        RANDOM,
        
        /**
         * 一致性哈希路由
         */
        CONSISTENT_HASH
    }
}
