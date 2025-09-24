package io.nebula.discovery.core;

/**
* 负载均衡策略枚举
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
    * 加权随机
    */
   WEIGHTED_RANDOM,
   
   /**
    * 最少活跃调用
    */
   LEAST_ACTIVE,
   
   /**
    * 一致性哈希
    */
   CONSISTENT_HASH,
   
   /**
    * 最快响应
    */
   FASTEST_RESPONSE
}