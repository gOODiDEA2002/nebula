package io.nebula.crawler.captcha.provider;

import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 2Captcha平台实现
 * API文档: https://2captcha.com/api-docs
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class TwoCaptchaProvider implements CaptchaServiceProvider {

    private static final String API_BASE = "https://2captcha.com";
    private static final Set<CaptchaType> SUPPORTED_TYPES = Set.of(
            CaptchaType.IMAGE,
            CaptchaType.RECAPTCHA,
            CaptchaType.HCAPTCHA,
            CaptchaType.SLIDER,
            CaptchaType.CLICK
    );

    private final String apiKey;
    private final OkHttpClient httpClient;

    public TwoCaptchaProvider(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "2Captcha";
    }

    @Override
    public boolean supportsType(CaptchaType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    public CaptchaResult solveImage(byte[] imageData, int timeout) throws CaptchaException {
        try {
            // 1. 提交验证码
            String base64 = Base64.getEncoder().encodeToString(imageData);

            FormBody submitBody = new FormBody.Builder()
                    .add("key", apiKey)
                    .add("method", "base64")
                    .add("body", base64)
                    .add("json", "1")
                    .build();

            Request submitRequest = new Request.Builder()
                    .url(API_BASE + "/in.php")
                    .post(submitBody)
                    .build();

            String taskId = submitTask(submitRequest);

            // 2. 轮询结果
            return pollResult(taskId, timeout, CaptchaType.IMAGE);

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha调用异常: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptchaResult solveRecaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException {
        try {
            FormBody submitBody = new FormBody.Builder()
                    .add("key", apiKey)
                    .add("method", "userrecaptcha")
                    .add("googlekey", siteKey)
                    .add("pageurl", siteUrl)
                    .add("json", "1")
                    .build();

            Request submitRequest = new Request.Builder()
                    .url(API_BASE + "/in.php")
                    .post(submitBody)
                    .build();

            String taskId = submitTask(submitRequest);
            CaptchaResult result = pollResult(taskId, timeout, CaptchaType.RECAPTCHA);

            // reCAPTCHA返回的是token
            if (result.isSuccess() && result.getText() != null) {
                result.setRecaptchaToken(result.getText());
            }

            return result;

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha reCAPTCHA识别异常: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptchaResult solveHcaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException {
        try {
            FormBody submitBody = new FormBody.Builder()
                    .add("key", apiKey)
                    .add("method", "hcaptcha")
                    .add("sitekey", siteKey)
                    .add("pageurl", siteUrl)
                    .add("json", "1")
                    .build();

            Request submitRequest = new Request.Builder()
                    .url(API_BASE + "/in.php")
                    .post(submitBody)
                    .build();

            String taskId = submitTask(submitRequest);
            CaptchaResult result = pollResult(taskId, timeout, CaptchaType.HCAPTCHA);

            if (result.isSuccess() && result.getText() != null) {
                result.setHcaptchaToken(result.getText());
            }

            return result;

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha hCaptcha识别异常: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptchaResult solveSlider(String backgroundBase64, String sliderBase64, int timeout) throws CaptchaException {
        // 2Captcha的滑块验证码需要使用coordinates方法
        throw new CaptchaException("2Captcha滑块识别需要使用coordinates方法，暂未实现");
    }

    @Override
    public CaptchaResult solveClick(String imageBase64, String hint, int timeout) throws CaptchaException {
        try {
            FormBody.Builder bodyBuilder = new FormBody.Builder()
                    .add("key", apiKey)
                    .add("method", "base64")
                    .add("coordinatescaptcha", "1")
                    .add("body", imageBase64)
                    .add("json", "1");

            if (hint != null && !hint.isEmpty()) {
                bodyBuilder.add("textinstructions", hint);
            }

            Request submitRequest = new Request.Builder()
                    .url(API_BASE + "/in.php")
                    .post(bodyBuilder.build())
                    .build();

            String taskId = submitTask(submitRequest);
            return pollResult(taskId, timeout, CaptchaType.CLICK);

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha点击验证码识别异常: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptchaResult solveGesture(String imageBase64, String hint, int timeout) throws CaptchaException {
        throw new CaptchaException("2Captcha暂不支持手势验证码识别");
    }

    /**
     * 提交任务
     */
    private String submitTask(Request request) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new CaptchaException("提交任务响应为空");
            }

            String result = body.string();
            log.debug("2Captcha提交响应: {}", result);

            // 解析JSON响应
            if (result.contains("\"status\":1")) {
                // 提取taskId
                int start = result.indexOf("\"request\":\"") + 11;
                int end = result.indexOf("\"", start);
                if (start > 11 && end > start) {
                    return result.substring(start, end);
                }
            }

            throw new CaptchaException("提交失败: " + result);
        }
    }

    /**
     * 轮询结果
     */
    private CaptchaResult pollResult(String taskId, int timeout, CaptchaType type) throws Exception {
        long startTime = System.currentTimeMillis();
        int pollInterval = 5000; // 5秒轮询一次

        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(pollInterval);

            Request request = new Request.Builder()
                    .url(API_BASE + "/res.php?key=" + apiKey + "&action=get&id=" + taskId + "&json=1")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (body == null) continue;

                String result = body.string();
                log.debug("2Captcha轮询响应: {}", result);

                if (result.contains("CAPCHA_NOT_READY")) {
                    continue;
                }

                if (result.contains("\"status\":1")) {
                    // 提取结果
                    int start = result.indexOf("\"request\":\"") + 11;
                    int end = result.indexOf("\"", start);
                    if (start > 11 && end > start) {
                        String text = result.substring(start, end);
                        return CaptchaResult.builder()
                                .success(true)
                                .type(type)
                                .text(text)
                                .taskId(taskId)
                                .build();
                    }
                }

                throw new CaptchaException("识别失败: " + result);
            }
        }

        throw new CaptchaException("识别超时");
    }

    @Override
    public void reportResult(String taskId, boolean success) {
        try {
            String action = success ? "reportgood" : "reportbad";
            Request request = new Request.Builder()
                    .url(API_BASE + "/res.php?key=" + apiKey + "&action=" + action + "&id=" + taskId)
                    .get()
                    .build();
            httpClient.newCall(request).execute().close();
            log.debug("报告结果成功: taskId={}, success={}", taskId, success);
        } catch (Exception e) {
            log.warn("报告结果失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return getBalance() > 0;
    }

    @Override
    public double getBalance() {
        try {
            Request request = new Request.Builder()
                    .url(API_BASE + "/res.php?key=" + apiKey + "&action=getbalance")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (body != null) {
                    return Double.parseDouble(body.string().trim());
                }
            }
        } catch (Exception e) {
            log.error("获取余额失败: {}", e.getMessage());
        }
        return 0;
    }
}
