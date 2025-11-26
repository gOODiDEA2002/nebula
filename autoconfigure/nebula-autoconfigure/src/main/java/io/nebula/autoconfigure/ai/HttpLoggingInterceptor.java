package io.nebula.autoconfigure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import io.nebula.core.common.util.Strings;

/**
 * HTTP请求/响应日志拦截器
 * 用于记录所有HTTP请求和响应的详细信息
 * 
 * 注意：需要配合 BufferingClientHttpRequestFactory 使用，以支持响应体的多次读取
 */
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        logRequest(request, body);
        long startTime = System.currentTimeMillis();

        ClientHttpResponse response = null;
        try {
            response = execution.execute(request, body);
            long endTime = System.currentTimeMillis();
            logResponse(request, response, endTime - startTime);
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("HTTP请求失败 - URL: {}, 耗时: {}ms, 错误: {}",
                    request.getURI(), endTime - startTime, e.getMessage());
            throw e;
        }
    }

    private void logRequest(HttpRequest request, byte[] body) {
        log.info("=== HTTP请求开始 ===");
        log.info("URI: {}", request.getURI());
        log.info("Method: {}", request.getMethod());
        log.info("Headers (共 {} 个):", request.getHeaders().size());
        if (request.getHeaders().isEmpty()) {
            log.warn("  (请求头为空!)");
        } else {
            request.getHeaders().forEach((name, values) ->
                    values.forEach(value -> log.info("  {}: {}", name, value)));
        }

        if (body != null && body.length > 0) {
            String bodyString = new String(body, StandardCharsets.UTF_8);
            String truncatedBodyString = Strings.truncate(bodyString, 20, "...");
            log.info("Request Body (长度: {} bytes): {}", body.length, truncatedBodyString);
        } else {
            log.warn("Request Body 为空");
        }
        log.info("===================");
    }

    private void logResponse(HttpRequest request, ClientHttpResponse response, long duration) {
        try {
            log.info("=== HTTP响应完成 ===");
            log.info("URI: {}", request.getURI());
            log.info("Status Code: {}", response.getStatusCode());
            log.info("Status Text: {}", response.getStatusText());
            log.info("Duration: {}ms", duration);
            log.info("Response Headers:");
            response.getHeaders().forEach((name, values) ->
                    values.forEach(value -> log.info("  {}: {}", name, value)));

            // 注意：不再读取响应体，因为使用SimpleClientHttpRequestFactory时响应流只能读取一次
            // 如果需要读取响应体，请使用BufferingClientHttpRequestFactory
            log.info("Response Body: (已跳过，避免消耗响应流)");
            log.info("====================");
        } catch (Exception e) {
            log.warn("记录HTTP响应时出错: {}", e.getMessage());
        }
    }

    private String readResponseBody(ClientHttpResponse response, int maxLength) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
            String body = reader.lines().collect(Collectors.joining("\n"));
            if (body.length() > maxLength) {
                return body.substring(0, maxLength) + "... (truncated)";
            }
            return body;
        } catch (Exception e) {
            return "(无法读取响应体: " + e.getMessage() + ")";
        }
    }
}

