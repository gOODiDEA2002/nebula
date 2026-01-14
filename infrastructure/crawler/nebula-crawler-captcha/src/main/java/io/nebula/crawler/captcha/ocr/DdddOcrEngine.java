package io.nebula.crawler.captcha.ocr;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ddddocr OCR引擎
 * 调用Python ddddocr服务
 * <p>
 * Python服务启动命令:
 * pip install ddddocr flask
 * python -m ddddocr server -p 8866
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class DdddOcrEngine implements OcrEngine {

    private final List<String> serverUrls;
    private final OkHttpClient httpClient;
    private final AtomicInteger counter = new AtomicInteger(0);

    public DdddOcrEngine(String serverUrl) {
        this(Collections.singletonList(serverUrl));
    }

    public DdddOcrEngine(List<String> serverUrls) {
        this.serverUrls = serverUrls.stream()
                .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .toList();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String getNextServerUrl() {
        if (serverUrls.isEmpty()) {
            throw new RuntimeException("没有可用的 ddddocr 服务地址");
        }
        if (serverUrls.size() == 1) {
            return serverUrls.get(0);
        }
        int index = Math.abs(counter.getAndIncrement() % serverUrls.size());
        return serverUrls.get(index);
    }

    @Override
    public String recognize(byte[] imageData) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageData);

        // 构建请求体
        MediaType mediaType = MediaType.parse("application/json");
        String jsonBody = "{\"image\":\"" + base64 + "\"}";
        RequestBody body = RequestBody.create(jsonBody, mediaType);

        Exception lastException = null;
        
        // 简单的重试机制：尝试最多 2 次 (或 URL 数量)
        int maxRetries = Math.min(2, serverUrls.size());
        if (serverUrls.size() > 1) {
             maxRetries = serverUrls.size(); // 如果有多个实例，尝试所有实例
        }

        for (int i = 0; i < maxRetries; i++) {
            String currentUrl = getNextServerUrl();
            try {
                // 构建请求
                Request request = new Request.Builder()
                        .url(currentUrl + "/ocr/b64/text")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("OCR服务响应错误: {} from {}", response.code(), currentUrl);
                        continue; // 尝试下一个
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                         log.warn("OCR服务响应为空 from {}", currentUrl);
                         continue;
                    }

                    String result = responseBody.string().trim();
                    log.debug("ddddocr识别结果: {}", result);

                    // 解析JSON响应（如果是JSON格式）
                    if (result.startsWith("{") && result.contains("\"result\"")) {
                        // 简单解析JSON
                        int start = result.indexOf("\"result\":\"") + 10;
                        int end = result.indexOf("\"", start);
                        if (start > 10 && end > start) {
                            result = result.substring(start, end);
                        }
                    }
                    return result;
                }
            } catch (IOException e) {
                log.warn("OCR服务调用失败: {} - {}", currentUrl, e.getMessage());
                lastException = e;
            }
        }
        
        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("OCR服务调用失败，所有实例均不可用");
    }

    @Override
    public String getName() {
        return "ddddocr";
    }

    @Override
    public boolean isAvailable() {
        for (String url : serverUrls) {
            try {
                Request request = new Request.Builder()
                        .url(url + "/ping")
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.debug("ddddocr服务不可用: {} - {}", url, e.getMessage());
            }
        }
        return false;
    }
}
