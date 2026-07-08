package io.nebula.autoconfigure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.search.core.SearchService;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;

/**
 * Elasticsearch 自动配置
 * <p>
 * 基于 elasticsearch-java 9.x + Rest5Client (HttpComponents 5) 构建。
 * Boot 4.1.0 BOM 统一管理 elasticsearch-client.version = 9.4.2。
 *
 * @author nebula
 */
@AutoConfiguration
@AutoConfigureBefore(name = {
    "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration",
    "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration",
    "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration"
})
@ConditionalOnClass(ElasticsearchClient.class)
@ConditionalOnProperty(prefix = "nebula.search.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAutoConfiguration.class);
    private static final java.util.Set<String> SAFE_PROFILES = java.util.Set.of("dev", "test", "local");

    private final ElasticsearchProperties properties;
    private final Environment environment;

    public ElasticsearchAutoConfiguration(ElasticsearchProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * 配置 Elasticsearch REST5 客户端（基于 HttpComponents 5）
     */
    @Bean
    @ConditionalOnMissingBean
    public Rest5Client elasticsearchRest5Client() {
        try {
            logger.info("Configuring Elasticsearch Rest5Client with uris: {}", properties.getUris());

            HttpHost[] hosts = properties.getUris().stream()
                    .map(uri -> {
                        try {
                            java.net.URI parsedUri = java.net.URI.create(uri);
                            String scheme = parsedUri.getScheme() != null ? parsedUri.getScheme() : "http";
                            int port = parsedUri.getPort() != -1 ? parsedUri.getPort() : 9200;
                            return new HttpHost(scheme, parsedUri.getHost(), port);
                        } catch (Exception e) {
                            logger.warn("Invalid Elasticsearch URI: {}, using localhost:9200", uri);
                            return new HttpHost("http", "localhost", 9200);
                        }
                    })
                    .toArray(HttpHost[]::new);

            Rest5ClientBuilder builder = Rest5Client.builder(hosts);

            builder.setConnectionConfigCallback(connectionConfigBuilder ->
                connectionConfigBuilder
                    .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(
                            properties.getConnectionTimeout().toMillis()))
                    .setSocketTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(
                            properties.getReadTimeout().toMillis()))
            );

            builder.setConnectionManagerCallback(connectionManagerBuilder ->
                connectionManagerBuilder
                    .setMaxConnTotal(properties.getMaxConnections())
                    .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
            );

            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                if (properties.getUsername() != null && properties.getPassword() != null) {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(null, -1),
                            new UsernamePasswordCredentials(
                                    properties.getUsername(),
                                    properties.getPassword().toCharArray()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            });

            if (properties.isSslEnabled()) {
                try {
                    builder.setSSLContext(createSSLContext());
                } catch (Exception e) {
                    logger.error("Failed to configure SSL for Elasticsearch client", e);
                    throw new RuntimeException("Failed to configure SSL", e);
                }
            }

            Rest5Client client = builder.build();
            logger.info("Elasticsearch Rest5Client configured successfully");
            return client;

        } catch (Exception e) {
            logger.error("Failed to configure Elasticsearch client", e);
            throw new RuntimeException("Failed to configure Elasticsearch client", e);
        }
    }

    /**
     * 配置 Elasticsearch 客户端（通过 Rest5ClientTransport 桥接）
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(Rest5Client rest5Client, ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        ElasticsearchTransport transport = new Rest5ClientTransport(rest5Client, jsonpMapper);
        return new ElasticsearchClient(transport);
    }

    /**
     * 配置 Nebula 搜索服务
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(SearchService.class)
    public SearchService searchService(ElasticsearchClient elasticsearchClient) {
        logger.info("Configuring Nebula Elasticsearch Search Service");
        return new ElasticsearchSearchService(elasticsearchClient, properties);
    }

    /**
     * 创建 SSL 上下文
     */
    private SSLContext createSSLContext() throws Exception {
        if (!properties.isSslVerificationEnabled()) {
            if (isProductionProfile()) {
                logger.error("生产环境禁止跳过 SSL 证书校验(sslVerificationEnabled=false), 已忽略该配置, 使用默认 SSL 校验");
            } else {
                logger.warn("SSL 证书校验已禁用, 当前环境: {}, 仅限非生产环境使用",
                        String.join(",", environment.getActiveProfiles()));
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                }}, null);
                return sslContext;
            }
        }

        if (properties.getSslCaPath() != null) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream fis = new FileInputStream(properties.getSslCaPath())) {
                Certificate ca = cf.generateCertificate(fis);
                trustStore.setCertificateEntry("ca", ca);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext;
        }

        if (properties.getSslCertificatePath() != null && properties.getSslKeyPath() != null) {
            logger.warn("Client certificate configuration not fully implemented. " +
                    "Please implement based on your certificate format.");
        }

        return SSLContext.getDefault();
    }

    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        for (String profile : activeProfiles) {
            if (SAFE_PROFILES.contains(profile.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 组件摘要: Elasticsearch
     */
    @Bean
    NebulaComponentSummary elasticsearchSummary() {
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("URIs", String.join(", ", properties.getUris()));
        details.put("Index Prefix", properties.getIndexPrefix());
        details.put("Shards", String.valueOf(properties.getDefaultShards()));
        details.put("Replicas", String.valueOf(properties.getDefaultReplicas()));
        details.put("Max Connections", String.valueOf(properties.getMaxConnections()));
        details.put("Connect Timeout", properties.getConnectionTimeout().toString());
        details.put("SSL Enabled", String.valueOf(properties.isSslEnabled()));
        return new SimpleComponentSummary("Search", "Elasticsearch", true, 400, details);
    }
}
