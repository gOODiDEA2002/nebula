package io.nebula.crawler.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
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
    
    @Override
    public CrawlerResponse crawl(CrawlerRequest request) {
        if (shutdown) {
            return CrawlerResponse.failure(request.getRequestId(), request.getUrl(),
                "引擎已关闭", null);
        }
        
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
            
            // 创建新页面
            page = context.newPage();
            
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
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("target closed") || errorMsg.contains("browser has been closed") ||
                errorMsg.contains("context has been closed") || errorMsg.contains("page has been closed")) {
                contextCorrupted = true;
            }
            
            // 错误时截图
            byte[] errorScreenshot = null;
            if (page != null && properties.isScreenshotOnError() && !contextCorrupted) {
                try {
                    errorScreenshot = page.screenshot();
                } catch (Exception screenshotError) {
                    log.warn("错误截图失败: {}", screenshotError.getMessage());
                    contextCorrupted = true; // 截图失败也说明上下文可能损坏
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
}

