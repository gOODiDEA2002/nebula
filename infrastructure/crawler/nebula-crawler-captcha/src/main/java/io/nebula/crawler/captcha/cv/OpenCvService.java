package io.nebula.crawler.captcha.cv;

import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenCV Python服务客户端
 * 提供滑块缺口检测和旋转角度检测功能
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class OpenCvService {

    private final List<String> serverUrls;
    private final OkHttpClient httpClient;
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param serverUrl OpenCV服务地址
     */
    public OpenCvService(String serverUrl) {
        this(Collections.singletonList(serverUrl));
    }

    /**
     * 构造函数
     *
     * @param serverUrls OpenCV服务地址列表
     */
    public OpenCvService(List<String> serverUrls) {
        this.serverUrls = serverUrls.stream()
                .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .toList();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String getNextServerUrl() {
        if (serverUrls.isEmpty()) {
            throw new RuntimeException("没有可用的 OpenCV 服务地址");
        }
        if (serverUrls.size() == 1) {
            return serverUrls.get(0);
        }
        int index = Math.abs(counter.getAndIncrement() % serverUrls.size());
        return serverUrls.get(index);
    }

    /**
     * 检测滑块缺口位置
     *
     * @param backgroundBase64 背景图Base64
     * @param sliderBase64     滑块图Base64
     * @return 滑块偏移量，检测失败返回null
     */
    public SliderDetectResult detectSliderGap(String backgroundBase64, String sliderBase64) {
        int maxRetries = Math.max(2, serverUrls.size());
        
        for (int i = 0; i < maxRetries; i++) {
            String currentUrl = getNextServerUrl();
            try {
                RequestBody body = new FormBody.Builder()
                        .add("background", backgroundBase64)
                        .add("slider", sliderBase64 != null ? sliderBase64 : "")
                        .build();

                Request request = new Request.Builder()
                        .url(currentUrl + "/slider/detect")
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("OpenCV滑块检测服务响应错误: {} from {}", response.code(), currentUrl);
                        continue;
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    return parseSliderResult(responseBody);
                }
            } catch (Exception e) {
                log.warn("调用OpenCV滑块检测服务失败: {} - {}", currentUrl, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 检测旋转角度
     *
     * @param imageBase64 图片Base64
     * @return 旋转角度，检测失败返回null
     */
    public RotateDetectResult detectRotateAngle(String imageBase64) {
        int maxRetries = Math.max(2, serverUrls.size());
        
        for (int i = 0; i < maxRetries; i++) {
            String currentUrl = getNextServerUrl();
            try {
                RequestBody body = new FormBody.Builder()
                        .add("image", imageBase64)
                        .build();

                Request request = new Request.Builder()
                        .url(currentUrl + "/rotate/detect")
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("OpenCV旋转检测服务响应错误: {} from {}", response.code(), currentUrl);
                        continue;
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    return parseRotateResult(responseBody);
                }
            } catch (Exception e) {
                log.warn("调用OpenCV旋转检测服务失败: {} - {}", currentUrl, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 检查服务是否可用
     *
     * @return true如果可用
     */
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
                log.debug("OpenCV服务不可用: {} - {}", url, e.getMessage());
            }
        }
        return false;
    }

    /**
     * 解析滑块检测结果
     * 期望格式: {"success": true, "offset": 123, "confidence": 0.95}
     */
    private SliderDetectResult parseSliderResult(String responseBody) {
        try {
            // 简单JSON解析，避免引入额外依赖
            if (!responseBody.contains("\"success\":true") && !responseBody.contains("\"success\": true")) {
                log.debug("滑块检测失败: {}", responseBody);
                return null;
            }

            int offset = extractIntValue(responseBody, "offset");
            double confidence = extractDoubleValue(responseBody, "confidence");

            return new SliderDetectResult(offset, confidence);
        } catch (Exception e) {
            log.warn("解析滑块检测结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析旋转检测结果
     * 期望格式: {"success": true, "angle": 45, "confidence": 0.8}
     */
    private RotateDetectResult parseRotateResult(String responseBody) {
        try {
            if (!responseBody.contains("\"success\":true") && !responseBody.contains("\"success\": true")) {
                log.debug("旋转检测失败: {}", responseBody);
                return null;
            }

            int angle = extractIntValue(responseBody, "angle");
            double confidence = extractDoubleValue(responseBody, "confidence");

            return new RotateDetectResult(angle, confidence);
        } catch (Exception e) {
            log.warn("解析旋转检测结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从JSON字符串中提取整数值
     */
    private int extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(-?\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new IllegalArgumentException("Key not found: " + key);
    }

    /**
     * 从JSON字符串中提取浮点值
     */
    private double extractDoubleValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(-?\\d+\\.?\\d*)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0; // confidence可选，默认0
    }

    /**
     * 滑块检测结果
     */
    public record SliderDetectResult(int offset, double confidence) {
    }

    /**
     * 旋转检测结果
     */
    public record RotateDetectResult(int angle, double confidence) {
    }
}
