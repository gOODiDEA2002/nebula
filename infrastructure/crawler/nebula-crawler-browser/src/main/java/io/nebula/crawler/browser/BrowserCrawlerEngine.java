package io.nebula.crawler.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.kihdev.playwright.stealth4j.Stealth4j;
import io.github.kihdev.playwright.stealth4j.Stealth4jConfig;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import io.nebula.crawler.browser.util.StealthHelper;
import io.nebula.crawler.core.*;
import io.nebula.crawler.core.proxy.Proxy;
import io.nebula.crawler.core.proxy.ProxyProvider;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 浏览器爬虫引擎实现
 * <p>
 * 基于Playwright实现动态页面渲染，适用于：
 * - 需要JavaScript渲染的页面
 * - 动态加载内容的网站
 * - 需要模拟用户交互的场景
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class BrowserCrawlerEngine implements CrawlerEngine {
    
    private final BrowserCrawlerProperties properties;
    private final BrowserPool browserPool;
    private final ProxyProvider proxyProvider;
    private volatile boolean shutdown = false;
    
    /**
     * 构造函数
     *
     * @param properties    浏览器爬虫配置
     * @param proxyProvider 代理提供者（可为null）
     */
    public BrowserCrawlerEngine(BrowserCrawlerProperties properties, ProxyProvider proxyProvider) {
        this.properties = properties;
        this.proxyProvider = proxyProvider;
        this.browserPool = new BrowserPool(properties);
        
        log.info("BrowserCrawlerEngine初始化完成: browserType={}, poolSize={}",
            properties.getBrowserType(), properties.getPoolSize());
    }
    
    @Override
    public CrawlerEngineType getType() {
        return CrawlerEngineType.BROWSER;
    }
    
    /** 连接类错误最大重试次数 */
    private static final int MAX_CONNECTION_RETRIES = 2;
    
    /** 重试间隔（毫秒） */
    private static final long RETRY_DELAY_MS = 1000;

    @Override
    public CrawlerResponse crawl(CrawlerRequest request) {
        if (shutdown) {
            return CrawlerResponse.failure(request.getRequestId(), request.getUrl(),
                "引擎已关闭", null);
        }
        
        // 连接类错误自动重试
        CrawlerResponse lastResponse = null;
        for (int attempt = 0; attempt <= MAX_CONNECTION_RETRIES; attempt++) {
            if (attempt > 0) {
                log.info("浏览器连接类错误，第 {} 次重试: url={}", attempt, request.getUrl());
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            lastResponse = doCrawl(request);
            
            // 成功或非连接类错误，直接返回
            if (lastResponse.isSuccess() || !isConnectionError(lastResponse)) {
                return lastResponse;
            }
        }
        
        return lastResponse;
    }
    
    /**
     * 判断响应是否为连接类错误（可通过重试恢复）
     */
    private boolean isConnectionError(CrawlerResponse response) {
        if (response.isSuccess()) {
            return false;
        }
        String errorMsg = response.getErrorMessage();
        if (errorMsg == null) {
            return true; // null 错误通常是 NPE，可能由连接问题导致
        }
        String lower = errorMsg.toLowerCase();
        return lower.contains("object doesn't exist")
                || lower.contains("target closed")
                || lower.contains("browser has been closed")
                || lower.contains("context has been closed")
                || lower.contains("page has been closed")
                || lower.contains("cannot find object")
                || lower.contains("connection refused")
                || lower.contains("websocket");
    }

    /**
     * 执行单次浏览器爬取
     */
    private CrawlerResponse doCrawl(CrawlerRequest request) {
        long startTime = System.currentTimeMillis();
        BrowserContext context = null;
        Page page = null;
        Proxy proxy = null;
        boolean contextCorrupted = false;
        
        try {
            // 获取代理
            if (proxyProvider != null && properties.isUseProxy()) {
                proxy = request.getProxy() != null ? request.getProxy() : proxyProvider.getProxy();
            }
            
            // 获取浏览器上下文
            context = browserPool.acquire(proxy);
            
            // 创建新页面（根据配置决定是否启用 stealth）
            page = createPage(context);
            
            // 设置超时
            page.setDefaultTimeout(properties.getPageTimeout());
            page.setDefaultNavigationTimeout(properties.getNavigationTimeout());
            
            // 设置请求头
            if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
                page.setExtraHTTPHeaders(request.getHeaders());
            }
            
            // 禁用图片/CSS（如果配置）
            if (properties.isDisableImages() || properties.isDisableCss()) {
                page.route("**/*", route -> {
                    String resourceType = route.request().resourceType();
                    if ((properties.isDisableImages() && "image".equals(resourceType)) ||
                        (properties.isDisableCss() && "stylesheet".equals(resourceType))) {
                        route.abort();
                    } else {
                        route.resume();
                    }
                });
            }
            
            // 导航到页面 - 默认使用COMMIT获得最快响应，避免等待资源加载
            WaitUntilState waitState = WaitUntilState.COMMIT;
            if (request.getWaitUntil() != null) {
                waitState = switch (request.getWaitUntil().toLowerCase()) {
                    case "load" -> WaitUntilState.LOAD;
                    case "networkidle" -> WaitUntilState.NETWORKIDLE;
                    case "domcontentloaded" -> WaitUntilState.DOMCONTENTLOADED;
                    default -> WaitUntilState.COMMIT;
                };
            }
            
            Response response = page.navigate(request.getUrl(), 
                new Page.NavigateOptions().setWaitUntil(waitState));
            
            // 等待元素（如果指定）
            if (request.getWaitSelector() != null && !request.getWaitSelector().isEmpty()) {
                page.waitForSelector(request.getWaitSelector(),
                    new Page.WaitForSelectorOptions().setTimeout(request.getWaitTimeout()));
            }
            
            // 获取页面内容
            String content = page.content();
            
            // 截图（如果请求）
            byte[] screenshot = null;
            if (request.isScreenshot()) {
                screenshot = page.screenshot();
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 构建响应
            CrawlerResponse crawlerResponse = CrawlerResponse.builder()
                .requestId(request.getRequestId())
                .url(request.getUrl())
                .finalUrl(page.url())
                .statusCode(response != null ? response.status() : 200)
                .content(content)
                .responseTime(responseTime)
                .success(true)
                .usedProxy(proxy)
                .screenshot(screenshot)
                .build();
            
            // 报告代理成功
            if (proxy != null && proxyProvider != null) {
                proxyProvider.reportSuccess(proxy);
            }
            
            log.debug("浏览器爬取成功: url={}, responseTime={}ms", request.getUrl(), responseTime);
            
            return crawlerResponse;
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("浏览器爬取失败: url={}, error={}", request.getUrl(), e.getMessage());
            
            // 检测是否是上下文损坏类型的错误
            contextCorrupted = isContextCorruptedError(e);
            
            // 错误时截图
            byte[] errorScreenshot = null;
            if (page != null && properties.isScreenshotOnError() && !contextCorrupted) {
                try {
                    errorScreenshot = page.screenshot();
                } catch (Exception screenshotError) {
                    log.warn("错误截图失败: {}", screenshotError.getMessage());
                    contextCorrupted = true;
                }
            }
            
            // 报告代理失败
            if (proxy != null && proxyProvider != null) {
                proxyProvider.reportFailure(proxy, e.getMessage());
            }
            
            return CrawlerResponse.builder()
                .requestId(request.getRequestId())
                .url(request.getUrl())
                .success(false)
                .errorMessage(e.getMessage())
                .exception(e)
                .responseTime(responseTime)
                .screenshot(errorScreenshot)
                .build();
            
        } finally {
            // 关闭页面
            if (page != null && !contextCorrupted) {
                try {
                    page.close();
                } catch (Exception ignored) {
                    contextCorrupted = true;
                }
            }
            
            // 释放上下文（传递损坏标记）
            if (context != null) {
                browserPool.release(context, contextCorrupted);
            }
        }
    }
    
    /**
     * 判断异常是否表示上下文/连接已损坏
     */
    private boolean isContextCorruptedError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMsg.contains("target closed")
                || errorMsg.contains("browser has been closed")
                || errorMsg.contains("context has been closed")
                || errorMsg.contains("page has been closed")
                || errorMsg.contains("object doesn't exist")
                || errorMsg.contains("cannot find object")
                || errorMsg.isEmpty(); // null message 通常是 NPE
    }
    
    @Override
    public CompletableFuture<CrawlerResponse> crawlAsync(CrawlerRequest request) {
        return CompletableFuture.supplyAsync(() -> crawl(request));
    }
    
    @Override
    public List<CrawlerResponse> crawlBatch(List<CrawlerRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 串行执行（浏览器资源有限）
        // 可根据poolSize决定并行度
        return requests.stream()
            .map(this::crawl)
            .collect(Collectors.toList());
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        browserPool.shutdown();
        log.info("BrowserCrawlerEngine已关闭");
    }
    
    @Override
    public boolean isHealthy() {
        return !shutdown && browserPool.isHealthy();
    }
    
    /**
     * 获取浏览器池统计信息
     */
    public BrowserPool.PoolStats getPoolStats() {
        return browserPool.getStats();
    }
    
    /**
     * 获取浏览器上下文（公开 API，供 LoginHandler 等组件使用）
     * 
     * @return 浏览器上下文
     * @throws InterruptedException 如果等待被中断
     */
    public BrowserContext acquireContext() throws InterruptedException {
        return browserPool.acquire(null);
    }
    
    /**
     * 释放浏览器上下文
     * 
     * @param context 浏览器上下文
     */
    public void releaseContext(BrowserContext context) {
        browserPool.release(context);
    }
    
    /**
     * 释放浏览器上下文（标记是否损坏）
     * 
     * @param context   浏览器上下文
     * @param corrupted 是否已损坏
     */
    public void releaseContext(BrowserContext context, boolean corrupted) {
        browserPool.release(context, corrupted);
    }
    
    /**
     * 创建带 Stealth 反检测的页面（公开 API）
     * 
     * @param context 浏览器上下文
     * @return 带反检测配置的页面
     */
    public Page createStealthPage(BrowserContext context) {
        return createPage(context);
    }
    
    /**
     * 创建页面（根据配置决定是否启用 Stealth）
     * 
     * @param context 浏览器上下文
     * @return 页面实例
     */
    private Page createPage(BrowserContext context) {
        BrowserCrawlerProperties.StealthConfig stealthConfig = properties.getStealth();
        
        if (stealthConfig == null || !stealthConfig.isEnabled()) {
            log.debug("Stealth 未启用，使用普通页面");
            return context.newPage();
        }
        
        try {
            // 使用 Stealth4j 默认配置（与 GongchangLoginTest 成功测试一致）
            // 默认配置启用所有 15 个 evasion，经过验证可以绕过腾讯验证码检测
            // 参考：Stealth4j.newStealthPage(context) 使用 Stealth4jConfig.DEFAULT
            Page page = Stealth4j.newStealthPage(context);
            log.debug("使用 Stealth4j 创建反检测页面（默认配置）");
            return page;
            
        } catch (Exception e) {
            log.warn("Stealth4j 创建页面失败，使用内置脚本: {}", e.getMessage());
            Page page = context.newPage();
            StealthHelper.applyBuiltinStealth(page);
            return page;
        }
    }
}

