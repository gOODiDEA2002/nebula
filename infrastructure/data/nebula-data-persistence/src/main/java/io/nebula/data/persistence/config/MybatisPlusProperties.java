package io.nebula.data.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis-Plus 配置属性
 * 支持在 nebula.data.persistence.mybatis-plus 下配置 MyBatis-Plus 相关参数
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.data.persistence.mybatis-plus")
public class MybatisPlusProperties {
    
    /**
     * Mapper XML 文件位置
     * 默认: classpath*:/mapper/**&#47;*.xml
     */
    private String mapperLocations = "classpath*:/mapper/**/*.xml";
    
    /**
     * 实体类别名包路径
     * 默认: io.nebula.**.entity
     */
    private String typeAliasesPackage = "io.nebula.**.entity";
    
    /**
     * 是否开启驼峰命名转换
     * 默认: true
     */
    private boolean mapUnderscoreToCamelCase = true;
    
    /**
     * SQL 日志实现类
     * 可选值: slf4j, stdout, no
     * 默认: slf4j
     */
    private String logImpl = "slf4j";
    
    /**
     * 全局配置
     */
    private GlobalConfig globalConfig = new GlobalConfig();
    
    /**
     * 全局配置
     */
    @Data
    public static class GlobalConfig {
        /**
         * 数据库配置
         */
        private DbConfig dbConfig = new DbConfig();
    }
    
    /**
     * 数据库配置
     */
    @Data
    public static class DbConfig {
        /**
         * 主键类型
         * 可选值: auto, none, input, assign_id, assign_uuid
         * 默认: auto
         */
        private String idType = "auto";
        
        /**
         * 逻辑删除字段名
         * 默认: deleted
         */
        private String logicDeleteField = "deleted";
        
        /**
         * 逻辑删除值（已删除）
         * 默认: 1
         */
        private Integer logicDeleteValue = 1;
        
        /**
         * 逻辑未删除值
         * 默认: 0
         */
        private Integer logicNotDeleteValue = 0;
        
        /**
         * 表名是否使用下划线
         * 默认: true
         */
        private boolean tableUnderline = true;
    }
    
    /**
     * 获取日志实现类全路径
     */
    public String getLogImplClass() {
        if (logImpl == null || logImpl.isEmpty()) {
            return null;
        }
        switch (logImpl.toLowerCase()) {
            case "slf4j":
                return "org.apache.ibatis.logging.slf4j.Slf4jImpl";
            case "stdout":
                return "org.apache.ibatis.logging.stdout.StdOutImpl";
            case "no":
            case "none":
                return "org.apache.ibatis.logging.nologging.NoLoggingImpl";
            default:
                // 如果是完整类名，直接返回
                return logImpl;
        }
    }
}

