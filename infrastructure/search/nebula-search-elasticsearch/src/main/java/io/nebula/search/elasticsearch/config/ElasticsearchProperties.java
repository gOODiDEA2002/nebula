package io.nebula.search.elasticsearch.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 配置属性
 *
 * @author nebula
 */
@ConfigurationProperties(prefix = "nebula.search.elasticsearch")
@Validated
public class ElasticsearchProperties {

    /**
     * 是否启用 Elasticsearch
     */
    private boolean enabled = true;

    /**
     * Elasticsearch 节点地址列表
     */
    @jakarta.validation.constraints.NotEmpty(message = "Elasticsearch uris cannot be empty")
    private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时时间
     */
    private Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * 读取超时时间
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * 最大连接数
     */
    @Min(value = 1, message = "Max connections must be at least 1")
    @Max(value = 1000, message = "Max connections must not exceed 1000")
    private int maxConnections = 100;

    /**
     * 每个路由的最大连接数
     */
    @Min(value = 1, message = "Max connections per route must be at least 1")
    @Max(value = 500, message = "Max connections per route must not exceed 500")
    private int maxConnectionsPerRoute = 10;

    /**
     * 默认索引前缀
     */
    private String indexPrefix = "nebula";

    /**
     * 默认分片数
     */
    @Min(value = 1, message = "Default shards must be at least 1")
    @Max(value = 100, message = "Default shards must not exceed 100")
    private int defaultShards = 1;

    /**
     * 默认副本数
     */
    @Min(value = 0, message = "Default replicas must be at least 0")
    @Max(value = 10, message = "Default replicas must not exceed 10")
    private int defaultReplicas = 1;

    /**
     * 批量操作大小
     */
    @Min(value = 1, message = "Bulk size must be at least 1")
    @Max(value = 10000, message = "Bulk size must not exceed 10000")
    private int bulkSize = 1000;

    /**
     * 批量操作超时时间
     */
    private Duration bulkTimeout = Duration.ofSeconds(30);

    /**
     * 搜索超时时间
     */
    private Duration searchTimeout = Duration.ofSeconds(30);

    /**
     * 滚动查询超时时间
     */
    private Duration scrollTimeout = Duration.ofMinutes(1);

    /**
     * 滚动查询大小
     */
    @Min(value = 1, message = "Scroll size must be at least 1")
    @Max(value = 10000, message = "Scroll size must not exceed 10000")
    private int scrollSize = 1000;

    /**
     * 是否启用 SSL
     */
    private boolean sslEnabled = false;

    /**
     * SSL 证书验证
     */
    private boolean sslVerificationEnabled = true;

    /**
     * SSL 证书路径
     */
    private String sslCertificatePath;

    /**
     * SSL 密钥路径
     */
    private String sslKeyPath;

    /**
     * SSL CA 证书路径
     */
    private String sslCaPath;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public String getIndexPrefix() {
        return indexPrefix;
    }

    public void setIndexPrefix(String indexPrefix) {
        this.indexPrefix = indexPrefix;
    }

    public int getDefaultShards() {
        return defaultShards;
    }

    public void setDefaultShards(int defaultShards) {
        this.defaultShards = defaultShards;
    }

    public int getDefaultReplicas() {
        return defaultReplicas;
    }

    public void setDefaultReplicas(int defaultReplicas) {
        this.defaultReplicas = defaultReplicas;
    }

    public int getBulkSize() {
        return bulkSize;
    }

    public void setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    public Duration getBulkTimeout() {
        return bulkTimeout;
    }

    public void setBulkTimeout(Duration bulkTimeout) {
        this.bulkTimeout = bulkTimeout;
    }

    public Duration getSearchTimeout() {
        return searchTimeout;
    }

    public void setSearchTimeout(Duration searchTimeout) {
        this.searchTimeout = searchTimeout;
    }

    public Duration getScrollTimeout() {
        return scrollTimeout;
    }

    public void setScrollTimeout(Duration scrollTimeout) {
        this.scrollTimeout = scrollTimeout;
    }

    public int getScrollSize() {
        return scrollSize;
    }

    public void setScrollSize(int scrollSize) {
        this.scrollSize = scrollSize;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public boolean isSslVerificationEnabled() {
        return sslVerificationEnabled;
    }

    public void setSslVerificationEnabled(boolean sslVerificationEnabled) {
        this.sslVerificationEnabled = sslVerificationEnabled;
    }

    public String getSslCertificatePath() {
        return sslCertificatePath;
    }

    public void setSslCertificatePath(String sslCertificatePath) {
        this.sslCertificatePath = sslCertificatePath;
    }

    public String getSslKeyPath() {
        return sslKeyPath;
    }

    public void setSslKeyPath(String sslKeyPath) {
        this.sslKeyPath = sslKeyPath;
    }

    public String getSslCaPath() {
        return sslCaPath;
    }

    public void setSslCaPath(String sslCaPath) {
        this.sslCaPath = sslCaPath;
    }
}

