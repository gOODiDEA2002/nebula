package io.nebula.crawler.http;

import io.nebula.crawler.core.*;
import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyProvider;
import io.nebula.crawler.core.ratelimit.CrawlerRateLimiter;
import io.nebula.crawler.http.config.HttpCrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * HTTP爬虫引擎实现
 * <p>
 * 基于OkHttp实现高性能HTTP爬取，支持：
 * - 连接池复用
 * - 自动重试
 * - 代理支持
 * - User-Agent轮换
 * - 限流控制
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class HttpCrawlerEngine implements CrawlerEngine {
    
    private final HttpCrawlerProperties properties;
    private final ProxyProvider proxyProvider;
    private final OkHttpClient httpClient;
    private final CrawlerRateLimiter rateLimiter;
    private volatile boolean shutdown = false;
    
    /**
     * 构造函数
     *
     * @param properties    HTTP爬虫配置
     * @param proxyProvider 代理提供者（可为null）
     */
    public HttpCrawlerEngine(HttpCrawlerProperties properties, ProxyProvider proxyProvider) {
        this.properties = properties;
        this.proxyProvider = proxyProvider;
        this.httpClient = buildHttpClient();
        this.rateLimiter = new CrawlerRateLimiter(properties.getDefaultQps());
        
        log.info("HttpCrawlerEngine初始化完成: connectTimeout={}ms, readTimeout={}ms, maxConnections={}",
            properties.getConnectTimeout(), properties.getReadTimeout(), properties.getMaxConnections());
    }
    
    /**
     * 构建OkHttp客户端
     */
    private OkHttpClient buildHttpClient() {
        ConnectionPool connectionPool = new ConnectionPool(
            properties.getMaxConnections(),
            properties.getKeepAliveTime(),
            TimeUnit.MILLISECONDS
        );
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)
            .connectionPool(connectionPool)
            .followRedirects(properties.isFollowRedirects())
            .followSslRedirects(properties.isFollowRedirects())
            .retryOnConnectionFailure(true);
        
        // 如果配置了信任所有证书，则忽略SSL验证（仅限测试环境）
        if (properties.isTrustAllCerts()) {
            log.warn("SSL证书验证已禁用，仅限测试环境使用！");
            configureUnsafeSsl(builder);
        }
        
        return builder.build();
    }
    
    /**
     * 配置忽略SSL证书验证（危险操作，仅限测试环境）
     */
    private void configureUnsafeSsl(OkHttpClient.Builder builder) {
        try {
            // 创建一个信任所有证书的TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // 不做任何校验
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // 不做任何校验
                    }
                    
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
            };
            
            // 创建SSLContext
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // 获取SSLSocketFactory
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            
        } catch (Exception e) {
            log.error("配置SSL忽略验证失败", e);
        }
    }
    
    @Override
    public CrawlerEngineType getType() {
        return CrawlerEngineType.HTTP;
    }
    
    @Override
    public CrawlerResponse crawl(CrawlerRequest request) {
        if (shutdown) {
            return CrawlerResponse.failure(request.getRequestId(), request.getUrl(),
                "引擎已关闭", null);
        }
        
        // 限流
        String domain = CrawlerRateLimiter.extractDomain(request.getUrl());
        rateLimiter.acquire(domain);
        
        long startTime = System.currentTimeMillis();
        Proxy proxy = null;
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= request.getRetryCount()) {
            try {
                // 获取代理
                if (proxyProvider != null && properties.isUseProxy()) {
                    proxy = request.getProxy() != null ? request.getProxy() : proxyProvider.getProxy();
                }
                
                // 构建OkHttp请求
                Request okRequest = buildOkHttpRequest(request);
                
                // 获取客户端（可能带代理）
                OkHttpClient client = getClientWithProxy(proxy, request);
                
                // 执行请求
                try (Response response = client.newCall(okRequest).execute()) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    CrawlerResponse crawlerResponse = buildCrawlerResponse(request, response, responseTime, proxy);
                    
                    // 报告代理成功
                    if (proxy != null && proxyProvider != null) {
                        proxyProvider.reportSuccess(proxy);
                    }
                    
                    log.debug("爬取成功: url={}, statusCode={}, responseTime={}ms",
                        request.getUrl(), response.code(), responseTime);
                    
                    return crawlerResponse;
                }
                
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                // 报告代理失败
                if (proxy != null && proxyProvider != null) {
                    proxyProvider.reportFailure(proxy, e.getMessage());
                    proxy = null; // 下次重试使用新代理
                }
                
                if (retryCount <= request.getRetryCount()) {
                    log.warn("爬取失败，准备重试: url={}, retry={}/{}, error={}",
                        request.getUrl(), retryCount, request.getRetryCount(), e.getMessage());
                    
                    try {
                        Thread.sleep(request.getRetryInterval());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败
        long responseTime = System.currentTimeMillis() - startTime;
        log.error("爬取失败（已用尽重试次数）: url={}, error={}",
            request.getUrl(), lastException != null ? lastException.getMessage() : "unknown");
        
        return CrawlerResponse.builder()
            .requestId(request.getRequestId())
            .url(request.getUrl())
            .success(false)
            .errorMessage(lastException != null ? lastException.getMessage() : "Unknown error")
            .exception(lastException)
            .responseTime(responseTime)
            .build();
    }
    
    @Override
    public CompletableFuture<CrawlerResponse> crawlAsync(CrawlerRequest request) {
        return CompletableFuture.supplyAsync(() -> crawl(request));
    }
    
    @Override
    public List<CrawlerResponse> crawlBatch(List<CrawlerRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 并行执行所有请求
        List<CompletableFuture<CrawlerResponse>> futures = requests.stream()
            .map(this::crawlAsync)
            .collect(Collectors.toList());
        
        // 等待所有请求完成
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
    
    /**
     * 构建OkHttp请求
     */
    private Request buildOkHttpRequest(CrawlerRequest request) {
        // 构建URL（包含参数）
        String url = request.buildFullUrl();
        
        Request.Builder builder = new Request.Builder().url(url);
        
        // 设置请求头
        builder.header("User-Agent", properties.getRandomUserAgent());
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        builder.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        builder.header("Accept-Encoding", "gzip, deflate");
        builder.header("Connection", "keep-alive");
        
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        
        // 设置请求方法和请求体
        switch (request.getMethod()) {
            case POST:
                builder.post(buildRequestBody(request));
                break;
            case PUT:
                builder.put(buildRequestBody(request));
                break;
            case DELETE:
                if (request.getBody() != null) {
                    builder.delete(buildRequestBody(request));
                } else {
                    builder.delete();
                }
                break;
            case PATCH:
                builder.patch(buildRequestBody(request));
                break;
            case HEAD:
                builder.head();
                break;
            default:
                builder.get();
        }
        
        return builder.build();
    }
    
    /**
     * 构建请求体
     */
    private RequestBody buildRequestBody(CrawlerRequest request) {
        String body = request.getBody();
        if (body == null) {
            body = "";
        }
        
        MediaType mediaType = MediaType.parse(request.getContentType());
        return RequestBody.create(body, mediaType);
    }
    
    /**
     * 获取带代理的客户端
     */
    private OkHttpClient getClientWithProxy(Proxy proxy, CrawlerRequest request) {
        OkHttpClient.Builder builder = httpClient.newBuilder();
        
        // 设置请求级别的超时
        if (request.getConnectTimeout() > 0) {
            builder.connectTimeout(request.getConnectTimeout(), TimeUnit.MILLISECONDS);
        }
        if (request.getReadTimeout() > 0) {
            builder.readTimeout(request.getReadTimeout(), TimeUnit.MILLISECONDS);
        }
        
        // 设置代理
        if (proxy != null) {
            java.net.Proxy javaProxy = new java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                new InetSocketAddress(proxy.getHost(), proxy.getPort())
            );
            builder.proxy(javaProxy);
            
            // 设置代理认证
            if (proxy.isAuthenticated()) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                    return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
                });
            }
        }
        
        return builder.build();
    }
    
    /**
     * 构建响应对象
     */
    private CrawlerResponse buildCrawlerResponse(CrawlerRequest request, Response response,
                                                  long responseTime, Proxy proxy) throws IOException {
        ResponseBody body = response.body();
        String content = body != null ? body.string() : null;
        
        return CrawlerResponse.builder()
            .requestId(request.getRequestId())
            .url(request.getUrl())
            .finalUrl(response.request().url().toString())
            .statusCode(response.code())
            .headers(response.headers().toMultimap())
            .content(content)
            .contentType(response.header("Content-Type"))
            .responseTime(responseTime)
            .success(response.isSuccessful())
            .usedProxy(proxy)
            .build();
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        
        // 关闭连接池
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        
        // 关闭Cache（如果有）
        try {
            Cache cache = httpClient.cache();
            if (cache != null) {
                cache.close();
            }
        } catch (IOException e) {
            log.warn("关闭缓存失败: {}", e.getMessage());
        }
        
        log.info("HttpCrawlerEngine已关闭");
    }
    
    @Override
    public boolean isHealthy() {
        return !shutdown && !httpClient.dispatcher().executorService().isShutdown();
    }
    
    /**
     * 获取连接池统计信息
     */
    public ConnectionPoolStats getConnectionPoolStats() {
        ConnectionPool pool = httpClient.connectionPool();
        return new ConnectionPoolStats(
            pool.connectionCount(),
            pool.idleConnectionCount()
        );
    }
    
    /**
     * 连接池统计信息
     */
    public record ConnectionPoolStats(int connectionCount, int idleConnectionCount) {
    }
}

