package io.nebula.crawler.captcha.ocr;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

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

    private final String serverUrl;
    private final OkHttpClient httpClient;

    public DdddOcrEngine(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String recognize(byte[] imageData) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageData);

        // 构建请求体
        MediaType mediaType = MediaType.parse("application/json");
        String jsonBody = "{\"image\":\"" + base64 + "\"}";
        RequestBody body = RequestBody.create(jsonBody, mediaType);

        // 构建请求
        Request request = new Request.Builder()
                .url(serverUrl + "/ocr/b64/text")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("OCR服务响应错误: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException("OCR服务响应为空");
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
    }

    @Override
    public String getName() {
        return "ddddocr";
    }

    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/ping")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.debug("ddddocr服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
