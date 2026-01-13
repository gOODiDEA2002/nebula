package io.nebula.crawler.http.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 日志拦截器
 * <p>
 * 记录HTTP请求和响应的详细信息
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class LoggingInterceptor implements Interceptor {

    /**
     * 日志级别
     */
    public enum Level {
        /**
         * 不记录日志
         */
        NONE,
        /**
         * 仅记录基本信息（URL、方法、状态码、耗时）
         */
        BASIC,
        /**
         * 记录请求头
         */
        HEADERS,
        /**
         * 记录完整内容（包括请求体和响应体）
         */
        BODY
    }

    private final Level level;
    private final int maxBodyLogLength;

    /**
     * 创建BASIC级别的日志拦截器
     */
    public LoggingInterceptor() {
        this(Level.BASIC);
    }

    /**
     * 创建指定级别的日志拦截器
     *
     * @param level 日志级别
     */
    public LoggingInterceptor(Level level) {
        this(level, 4096);
    }

    /**
     * 创建日志拦截器
     *
     * @param level            日志级别
     * @param maxBodyLogLength 最大记录的请求体/响应体长度
     */
    public LoggingInterceptor(Level level, int maxBodyLogLength) {
        this.level = level != null ? level : Level.BASIC;
        this.maxBodyLogLength = maxBodyLogLength;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (level == Level.NONE) {
            return chain.proceed(chain.request());
        }

        Request request = chain.request();
        long startTime = System.nanoTime();

        // 记录请求
        logRequest(request);

        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            log.error("<-- HTTP FAILED: {} {}, error={}", 
                request.method(), request.url(), e.getMessage());
            throw e;
        }

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        // 记录响应
        return logResponse(response, duration);
    }

    /**
     * 记录请求信息
     */
    private void logRequest(Request request) {
        log.info("--> {} {} {}", 
            request.method(), 
            request.url(),
            request.body() != null ? "(" + request.body().contentType() + ")" : "");

        if (level.ordinal() >= Level.HEADERS.ordinal()) {
            request.headers().forEach(header -> 
                log.debug("    {}: {}", header.getFirst(), header.getSecond()));
        }

        if (level == Level.BODY && request.body() != null) {
            try {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String body = buffer.readString(StandardCharsets.UTF_8);
                if (body.length() > maxBodyLogLength) {
                    body = body.substring(0, maxBodyLogLength) + "...[truncated]";
                }
                log.debug("    Body: {}", body);
            } catch (Exception e) {
                log.debug("    Body: [无法读取]");
            }
        }
    }

    /**
     * 记录响应信息
     */
    private Response logResponse(Response response, long durationMs) throws IOException {
        log.info("<-- {} {} ({}ms, {})", 
            response.code(),
            response.request().url(),
            durationMs,
            response.message());

        if (level.ordinal() >= Level.HEADERS.ordinal()) {
            response.headers().forEach(header -> 
                log.debug("    {}: {}", header.getFirst(), header.getSecond()));
        }

        if (level == Level.BODY) {
            ResponseBody body = response.body();
            if (body != null) {
                String content = body.string();
                String logContent = content.length() > maxBodyLogLength ?
                    content.substring(0, maxBodyLogLength) + "...[truncated]" : content;
                log.debug("    Body: {}", logContent);

                // 重新构建响应（因为body只能读取一次）
                return response.newBuilder()
                    .body(ResponseBody.create(content, body.contentType()))
                    .build();
            }
        }

        return response;
    }

    /**
     * 获取日志级别
     */
    public Level getLevel() {
        return level;
    }
}

