package io.nebula.crawler.proxy.provider;

import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyType;
import io.nebula.crawler.proxy.config.ProxyPoolProperties;
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
 * proxy-pool 服务代理源
 * <p>
 * 从 proxy-pool 服务获取匿名代理
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class ProxyPoolApiSource implements ProxySource {

    private static final String SOURCE_NAME = "proxy-pool";

    private final ProxyPoolProperties.ProxyPoolService config;
    private final OkHttpClient httpClient;

    public ProxyPoolApiSource(ProxyPoolProperties.ProxyPoolService config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public String getName() {
        return SOURCE_NAME;
    }

    @Override
    public String getType() {
        return "proxy-pool-api";
    }

    @Override
    public List<Proxy> fetch() {
        List<Proxy> proxies = new ArrayList<>();

        if (!config.isEnabled()) {
            log.debug("ProxyPoolApiSource 未启用");
            return proxies;
        }

        // 只获取匿名代理
        String apiUrl = config.getBaseUrl() + "/all_anonymous/";

        try {
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    proxies = parseResponse(body);
                    log.info("ProxyPoolApiSource 获取到 {} 个匿名代理", proxies.size());
                } else {
                    log.warn("ProxyPoolApiSource 请求失败: status={}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("ProxyPoolApiSource 获取代理异常: {}", e.getMessage());
        }

        return proxies;
    }

    /**
     * 解析 proxy-pool 响应
     * <p>
     * 响应格式: [{"proxy": "ip:port", "https": true, ...}, ...]
     * </p>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Proxy> parseResponse(String responseBody) {
        List<Proxy> proxies = new ArrayList<>();

        if (responseBody == null || responseBody.isEmpty()) {
            return proxies;
        }

        try {
            List<Map> rawItems = JsonUtils.toList(responseBody, Map.class);
            for (Map rawItem : rawItems) {
                Map<String, Object> item = (Map<String, Object>) rawItem;
                Proxy proxy = parseProxyItem(item);
                if (proxy != null) {
                    proxies.add(proxy);
                }
            }
        } catch (Exception e) {
            log.warn("解析 proxy-pool 响应失败: {}", e.getMessage());
        }

        return proxies;
    }

    /**
     * 解析单个代理项
     */
    private Proxy parseProxyItem(Map<String, Object> item) {
        try {
            // proxy 字段格式: "ip:port"
            String proxyStr = (String) item.get("proxy");
            if (proxyStr == null || proxyStr.isEmpty()) {
                return null;
            }

            String[] parts = proxyStr.split(":");
            if (parts.length < 2) {
                return null;
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // 判断是否支持 HTTPS
            Boolean https = (Boolean) item.get("https");
            ProxyType type = (https != null && https) ? ProxyType.HTTPS : ProxyType.HTTP;

            return Proxy.builder()
                    .host(host)
                    .port(port)
                    .type(type)
                    .build();
        } catch (Exception e) {
            log.debug("解析代理项失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
