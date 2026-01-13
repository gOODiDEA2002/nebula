package io.nebula.crawler.proxy;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyValidator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 代理检测器
 * <p>
 * 检测代理的可用性和响应时间
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ProxyChecker implements ProxyValidator {
    
    private final String checkUrl;
    private final int timeout;
    private final OkHttpClient baseClient;
    
    /**
     * 构造函数
     *
     * @param checkUrl 检测URL
     * @param timeout  超时时间(ms)
     */
    public ProxyChecker(String checkUrl, int timeout) {
        this.checkUrl = checkUrl;
        this.timeout = timeout;
        this.baseClient = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build();
    }
    
    @Override
    public ValidationResult validate(Proxy proxy) {
        if (proxy == null) {
            return ValidationResult.failure("代理为空");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建带代理的客户端
            java.net.Proxy javaProxy = new java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                new InetSocketAddress(proxy.getHost(), proxy.getPort())
            );
            
            OkHttpClient client = baseClient.newBuilder()
                .proxy(javaProxy)
                .build();
            
            // 发送请求
            Request request = new Request.Builder()
                .url(checkUrl)
                .get()
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                if (response.isSuccessful()) {
                    log.debug("代理检测成功: {}, responseTime={}ms", proxy.toAddress(), responseTime);
                    return ValidationResult.success(responseTime);
                } else {
                    String message = String.format("状态码: %d", response.code());
                    log.debug("代理检测失败: {}, {}", proxy.toAddress(), message);
                    return ValidationResult.failure(message);
                }
            }
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("代理检测异常: {}, error={}, time={}ms", 
                proxy.toAddress(), e.getMessage(), responseTime);
            return ValidationResult.failure(e.getMessage());
        }
    }
    
    /**
     * 批量检测代理
     *
     * @param proxies 代理列表
     * @return 可用代理列表
     */
    public List<Proxy> validateBatch(List<Proxy> proxies) {
        return proxies.parallelStream()
            .filter(proxy -> {
                ValidationResult result = validate(proxy);
                if (result.isValid()) {
                    proxy.updateResponseTime(result.responseTime());
                    return true;
                }
                return false;
            })
            .toList();
    }
}

