package io.nebula.crawler.proxy.provider;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyType;
import io.nebula.core.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API代理来源
 * <p>
 * 从第三方代理API获取代理列表
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ApiProxySource implements ProxySource {
    
    private final String name;
    private final String apiUrl;
    private final int priority;
    private final ResponseParser parser;
    private final OkHttpClient httpClient;
    
    /**
     * 构造函数
     *
     * @param name    来源名称
     * @param apiUrl  API URL
     * @param parser  响应解析器
     * @param priority 优先级
     */
    public ApiProxySource(String name, String apiUrl, ResponseParser parser, int priority) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.parser = parser;
        this.priority = priority;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getType() {
        return "api";
    }
    
    @Override
    public List<Proxy> fetch() {
        List<Proxy> proxies = new ArrayList<>();
        
        try {
            Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    proxies = parser.parse(body);
                    log.info("ApiProxySource[{}] 获取到 {} 个代理", name, proxies.size());
                } else {
                    log.warn("ApiProxySource[{}] 请求失败: status={}", name, response.code());
                }
            }
        } catch (Exception e) {
            log.error("ApiProxySource[{}] 获取代理异常: {}", name, e.getMessage());
        }
        
        return proxies;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    /**
     * 响应解析器接口
     */
    @FunctionalInterface
    public interface ResponseParser {
        /**
         * 解析响应内容
         *
         * @param responseBody 响应体
         * @return 代理列表
         */
        List<Proxy> parse(String responseBody);
    }
    
    /**
     * 简单文本解析器 - 每行一个代理
     */
    public static class SimpleTextParser implements ResponseParser {
        @Override
        public List<Proxy> parse(String responseBody) {
            List<Proxy> proxies = new ArrayList<>();
            if (responseBody == null || responseBody.isEmpty()) {
                return proxies;
            }
            
            String[] lines = responseBody.split("\n");
            for (String line : lines) {
                Proxy proxy = Proxy.parse(line.trim());
                if (proxy != null) {
                    proxies.add(proxy);
                }
            }
            return proxies;
        }
    }
    
    /**
     * JSON数组解析器 - 标准JSON格式
     */
    public static class JsonArrayParser implements ResponseParser {
        
        private final String hostField;
        private final String portField;
        private final String usernameField;
        private final String passwordField;
        
        public JsonArrayParser() {
            this("host", "port", "username", "password");
        }
        
        public JsonArrayParser(String hostField, String portField, 
                               String usernameField, String passwordField) {
            this.hostField = hostField;
            this.portField = portField;
            this.usernameField = usernameField;
            this.passwordField = passwordField;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public List<Proxy> parse(String responseBody) {
            List<Proxy> proxies = new ArrayList<>();
            if (responseBody == null || responseBody.isEmpty()) {
                return proxies;
            }
            
            try {
                @SuppressWarnings("rawtypes")
                List<Map> rawItems = JsonUtils.toList(responseBody, Map.class);
                for (Map rawItem : rawItems) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) rawItem;
                    String host = String.valueOf(item.get(hostField));
                    int port = Integer.parseInt(String.valueOf(item.get(portField)));
                    
                    Proxy.ProxyBuilder builder = Proxy.builder()
                        .host(host)
                        .port(port)
                        .type(ProxyType.HTTP);
                    
                    if (item.containsKey(usernameField) && item.containsKey(passwordField)) {
                        builder.username(String.valueOf(item.get(usernameField)))
                               .password(String.valueOf(item.get(passwordField)))
                               .authenticated(true);
                    }
                    
                    proxies.add(builder.build());
                }
            } catch (Exception e) {
                log.warn("解析JSON代理列表失败: {}", e.getMessage());
            }
            
            return proxies;
        }
    }
}

