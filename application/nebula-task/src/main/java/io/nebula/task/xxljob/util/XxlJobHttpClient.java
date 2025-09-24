package io.nebula.task.xxljob.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * XXL-JOB HTTP 客户端
 * 用于与 XXL-JOB 管理端通信
 */
@Component
public class XxlJobHttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(XxlJobHttpClient.class);
    private static final int TIMEOUT_SECONDS = 30;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public XxlJobHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 发送 POST 请求
     * 
     * @param url 请求地址
     * @param requestData 请求数据
     * @param accessToken 访问令牌
     * @param responseClass 响应类型
     * @return 响应对象
     */
    public <T> T post(String url, Object requestData, String accessToken, Class<T> responseClass) {
        try {
            // 序列化请求数据
            String jsonData = objectMapper.writeValueAsString(requestData);
            
            RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, jsonData);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(requestBody);
            
            // 添加访问令牌
            if (accessToken != null && !accessToken.isEmpty()) {
                requestBuilder.addHeader("XXL-JOB-ACCESS-TOKEN", accessToken);
            }
            
            Request request = requestBuilder.build();
            
            logger.debug("发送 POST 请求: url={}, data={}", url, jsonData);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("HTTP 请求失败: url={}, code={}, message={}", url, response.code(), response.message());
                    return null;
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    logger.warn("HTTP 响应体为空: url={}", url);
                    return null;
                }
                
                String responseText = responseBody.string();
                logger.debug("收到 HTTP 响应: url={}, response={}", url, responseText);
                
                // 反序列化响应数据
                return objectMapper.readValue(responseText, responseClass);
            }
            
        } catch (IOException e) {
            logger.error("HTTP 请求异常: url={}, error={}", url, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("请求处理异常: url={}, error={}", url, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 发送 GET 请求
     * 
     * @param url 请求地址
     * @param accessToken 访问令牌
     * @param responseClass 响应类型
     * @return 响应对象
     */
    public <T> T get(String url, String accessToken, Class<T> responseClass) {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();
            
            // 添加访问令牌
            if (accessToken != null && !accessToken.isEmpty()) {
                requestBuilder.addHeader("XXL-JOB-ACCESS-TOKEN", accessToken);
            }
            
            Request request = requestBuilder.build();
            
            logger.debug("发送 GET 请求: url={}", url);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("HTTP 请求失败: url={}, code={}, message={}", url, response.code(), response.message());
                    return null;
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    logger.warn("HTTP 响应体为空: url={}", url);
                    return null;
                }
                
                String responseText = responseBody.string();
                logger.debug("收到 HTTP 响应: url={}, response={}", url, responseText);
                
                // 反序列化响应数据
                return objectMapper.readValue(responseText, responseClass);
            }
            
        } catch (IOException e) {
            logger.error("HTTP 请求异常: url={}, error={}", url, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("请求处理异常: url={}, error={}", url, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
