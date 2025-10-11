package io.nebula.autoconfigure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.search.core.SearchService;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;

/**
 * Elasticsearch 自动配置
 *
 * @author nebula
 */
@AutoConfiguration(after = ElasticsearchDataAutoConfiguration.class)
@ConditionalOnClass({ElasticsearchClient.class, ElasticsearchOperations.class})
@ConditionalOnProperty(prefix = "nebula.search.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAutoConfiguration.class);

    private final ElasticsearchProperties properties;

    public ElasticsearchAutoConfiguration(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 Elasticsearch REST 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient elasticsearchRestClient() {
        try {
            logger.info("Configuring Elasticsearch client with uris: {}", properties.getUris());
            
            List<HttpHost> hosts = properties.getUris().stream()
                .map(uri -> {
                    try {
                        java.net.URI parsedUri = java.net.URI.create(uri);
                        return new HttpHost(
                            parsedUri.getHost(),
                            parsedUri.getPort() != -1 ? parsedUri.getPort() : 9200,
                            parsedUri.getScheme()
                        );
                    } catch (Exception e) {
                        logger.warn("Invalid Elasticsearch URI: {}, using localhost:9200", uri);
                        return new HttpHost("localhost", 9200, "http");
                    }
                })
                .toList();

            RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

            // 配置认证
            if (properties.getUsername() != null && properties.getPassword() != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
                );
                
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );
            }

            // 配置连接
            builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout((int) properties.getConnectionTimeout().toMillis())
                    .setSocketTimeout((int) properties.getReadTimeout().toMillis())
            );

            // 配置连接池
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder
                    .setMaxConnTotal(properties.getMaxConnections())
                    .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
            );

            // 配置 SSL
            if (properties.isSslEnabled()) {
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    try {
                        SSLContext sslContext = createSSLContext();
                        return httpClientBuilder.setSSLContext(sslContext);
                    } catch (Exception e) {
                        logger.error("Failed to configure SSL for Elasticsearch client", e);
                        throw new RuntimeException("Failed to configure SSL", e);
                    }
                });
            }

            RestClient client = builder.build();
            logger.info("Elasticsearch REST client configured successfully");
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to configure Elasticsearch client", e);
            throw new RuntimeException("Failed to configure Elasticsearch client", e);
        }
    }

    /**
     * 配置 Elasticsearch 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(RestClient restClient, ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        //
        ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);
        return new ElasticsearchClient(transport);
    }

    /**
     * 配置 Nebula 搜索服务
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(SearchService.class)
    public SearchService searchService(ElasticsearchClient elasticsearchClient,
                                       ElasticsearchOperations elasticsearchOperations) {
        logger.info("Configuring Nebula Elasticsearch Search Service");
        return new ElasticsearchSearchService(elasticsearchClient, elasticsearchOperations, properties);
    }

    /**
     * 创建 SSL 上下文
     */
    private SSLContext createSSLContext() throws Exception {
        SSLContextBuilder sslContextBuilder = SSLContexts.custom();

        // 如果禁用 SSL 验证
        if (!properties.isSslVerificationEnabled()) {
            sslContextBuilder.loadTrustMaterial(null, (certificate, authType) -> true);
            return sslContextBuilder.build();
        }

        // 加载证书
        if (properties.getSslCaPath() != null) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream fis = new FileInputStream(properties.getSslCaPath())) {
                Certificate ca = cf.generateCertificate(fis);
                trustStore.setCertificateEntry("ca", ca);
            }

            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }

        // 加载客户端证书
        if (properties.getSslCertificatePath() != null && properties.getSslKeyPath() != null) {
            // 这里需要根据具体的证书格式实现
            // 由于证书格式可能多样，这里提供基础框架
            logger.warn("Client certificate configuration not fully implemented. " +
                       "Please implement based on your certificate format.");
        }

        return sslContextBuilder.build();
    }
}
