package io.nebula.autoconfigure.rpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步RPC配置属性
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.rpc.async")
public class AsyncRpcProperties {

    /**
     * 是否启用异步RPC
     */
    private boolean enabled = true;

    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();

    /**
     * 执行器配置
     */
    private ExecutorConfig executor = new ExecutorConfig();

    /**
     * 清理策略配置
     */
    private CleanupConfig cleanup = new CleanupConfig();

    @Data
    public static class StorageConfig {
        /**
         * 存储类型：nacos(默认) / redis / database
         */
        private String type = "nacos";

        /**
         * Nacos配置（当type=nacos时生效）
         */
        private NacosConfig nacos = new NacosConfig();

        /**
         * Redis配置
         */
        private RedisConfig redis = new RedisConfig();
    }

    @Data
    public static class NacosConfig {
        /**
         * Nacos服务地址
         */
        private String serverAddr = "localhost:8848";

        /**
         * 命名空间
         */
        private String namespace = "public";

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";
    }

    @Data
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
    }

    @Data
    public static class ExecutorConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 10;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 50;

        /**
         * 队列容量
         */
        private int queueCapacity = 200;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "async-rpc-";
    }

    @Data
    public static class CleanupConfig {
        /**
         * 是否启用自动清理
         */
        private boolean enabled = true;

        /**
         * 保留天数
         */
        private int retentionDays = 7;
    }
}
