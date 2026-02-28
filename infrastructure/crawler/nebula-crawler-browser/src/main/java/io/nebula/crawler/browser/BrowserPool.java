package io.nebula.crawler.browser;

import com.microsoft.playwright.*;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties.Mode;
import io.nebula.crawler.core.proxy.Proxy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 浏览器实例池
 * <p>
 * 管理 Playwright 浏览器实例的创建和复用，支持两种模式：
 * <ul>
 * <li>LOCAL - 本地启动浏览器实例，预创建 Context 池复用</li>
 * <li>REMOTE - 连接到远程 Playwright Server，每个活跃 Context 独占一个 Browser 连接，
 *     避免多线程并发操作同一 WebSocket 通道导致的 "Object doesn't exist" 错误</li>
 * </ul>
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class BrowserPool {

    private final BrowserCrawlerProperties properties;
    private final Playwright playwright;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /** 获取 Browser 的超时时间（秒），从配置中读取 */
    private final int acquireTimeoutSeconds;

    /** 重连锁，防止多线程同时触发重连 */
    private final ReentrantLock reconnectLock = new ReentrantLock();

    // ==================== 本地模式 ====================
    private Browser localBrowser;
    /** 本地模式预创建的 Context 池 */
    private final BlockingQueue<BrowserContext> contextPool;

    // ==================== 远程模式 ====================
    /**
     * Browser 独占队列：每次 acquire 从中取出一个 Browser，使用完毕后归还。
     * 保证每个活跃 BrowserContext 独占一个 Browser 连接（WebSocket），
     * 从根本上避免 Playwright Java 客户端多线程并发操作同一连接的线程安全问题。
     */
    private final BlockingQueue<Browser> browserQueue = new LinkedBlockingQueue<>();
    /** context -> browser 映射，release 时归还对应的 Browser */
    private final ConcurrentHashMap<BrowserContext, Browser> contextBrowserMap = new ConcurrentHashMap<>();
    /** 端点到其 Browser 连接列表的映射（用于健康检查和重连） */
    private final Map<String, List<Browser>> endpointBrowsers = new ConcurrentHashMap<>();
    /** 所有 Browser 连接的引用（用于 shutdown） */
    private final List<Browser> allBrowserConnections = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private ScheduledExecutorService healthCheckExecutor;

    public BrowserPool(BrowserCrawlerProperties properties) {
        this.properties = properties;
        this.acquireTimeoutSeconds = Math.max(10, properties.getAcquireTimeout());

        if (properties.isRemoteMode()) {
            log.info("远程模式启用，跳过本地浏览器下载");
            this.playwright = Playwright.create(new Playwright.CreateOptions()
                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        } else {
            this.playwright = Playwright.create();
        }

        this.contextPool = new LinkedBlockingQueue<>(properties.getPoolSize());

        if (properties.isRemoteMode()) {
            initializeRemoteMode();
        } else {
            initializeLocalMode();
        }

        log.info("BrowserPool 初始化完成: mode={}, browserType={}, poolSize={}",
                properties.getMode(), properties.getBrowserType(), properties.getPoolSize());
    }

    // ==================== 初始化 ====================

    private void initializeLocalMode() {
        this.localBrowser = createLocalBrowser();
        initializeContextPool();
        log.info("本地浏览器模式初始化完成: headless={}", properties.isHeadless());
    }

    private void initializeRemoteMode() {
        List<String> endpoints = properties.getRemote().getEndpoints();
        if (endpoints.isEmpty()) {
            throw new IllegalStateException("远程模式需要配置至少一个端点 (nebula.crawler.browser.remote.endpoints)");
        }

        int connsPerEndpoint = Math.max(1, properties.getRemote().getConnectionsPerEndpoint());

        for (String endpoint : endpoints) {
            List<Browser> browsers = new CopyOnWriteArrayList<>();
            for (int i = 0; i < connsPerEndpoint; i++) {
                Browser browser = connectSingle(endpoint, i);
                if (browser != null) {
                    browsers.add(browser);
                    allBrowserConnections.add(browser);
                    browserQueue.offer(browser);
                }
            }
            endpointBrowsers.put(endpoint, browsers);
        }

        if (allBrowserConnections.isEmpty()) {
            throw new IllegalStateException("无法连接到任何远程端点");
        }

        // 远程模式不预创建 Context 池：
        // 每个 Context 需独占一个 Browser 连接以避免并发冲突

        startHealthCheck();

        log.info("远程浏览器模式初始化完成: endpoints={}, connectionsPerEndpoint={}, totalConnections={}, browserQueue={}",
                endpoints.size(), connsPerEndpoint, allBrowserConnections.size(), browserQueue.size());
    }

    private Browser connectSingle(String endpoint, int index) {
        try {
            Browser browser;
            if (properties.getRemote().isUseCdp()) {
                String cdpEndpoint = endpoint.replace("ws://", "http://");
                browser = playwright.chromium().connectOverCDP(cdpEndpoint,
                        new BrowserType.ConnectOverCDPOptions()
                                .setTimeout(properties.getConnectTimeout()));
                log.info("CDP 连接成功: endpoint={}, index={}", cdpEndpoint, index);
            } else {
                browser = playwright.chromium().connect(endpoint,
                        new BrowserType.ConnectOptions()
                                .setTimeout(properties.getConnectTimeout()));
                log.info("Playwright Server 连接成功: endpoint={}, index={}", endpoint, index);
            }
            return browser;
        } catch (Exception e) {
            log.error("Browser 连接失败: endpoint={}, index={}, error={}", endpoint, index, e.getMessage());
            return null;
        }
    }

    private void startHealthCheck() {
        int interval = properties.getRemote().getHealthCheckInterval();
        if (interval <= 0) {
            return;
        }

        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "browser-health-check");
            t.setDaemon(true);
            return t;
        });

        healthCheckExecutor.scheduleAtFixedRate(this::performHealthCheck,
                interval, interval, TimeUnit.MILLISECONDS);

        log.info("健康检查已启动: interval={}ms", interval);
    }

    /**
     * 执行健康检查 -- 检查每个端点的每个连接，断开的进行重连
     */
    private void performHealthCheck() {
        if (shutdown.get()) {
            return;
        }

        for (Map.Entry<String, List<Browser>> entry : endpointBrowsers.entrySet()) {
            String endpoint = entry.getKey();
            List<Browser> browsers = entry.getValue();

            for (int i = 0; i < browsers.size(); i++) {
                Browser browser = browsers.get(i);
                if (browser == null || !browser.isConnected()) {
                    log.warn("Browser 连接断开，尝试重连: endpoint={}, index={}", endpoint, i);

                    // 从队列中移除旧 Browser（如果还在队列中）
                    browserQueue.remove(browser);
                    allBrowserConnections.remove(browser);
                    if (browser != null) {
                        try {
                            browser.close();
                        } catch (Exception ignored) {
                        }
                    }

                    Browser newBrowser = connectSingle(endpoint, i);
                    if (newBrowser != null) {
                        browsers.set(i, newBrowser);
                        allBrowserConnections.add(newBrowser);
                        browserQueue.offer(newBrowser);
                        log.info("Browser 重连成功: endpoint={}, index={}", endpoint, i);
                    }
                }
            }
        }
    }

    private boolean hasAnyConnectedBrowser() {
        if (properties.isLocalMode()) {
            return localBrowser != null && localBrowser.isConnected();
        }
        for (Browser browser : allBrowserConnections) {
            if (browser != null && browser.isConnected()) {
                return true;
            }
        }
        return false;
    }

    private Browser createLocalBrowser() {
        BrowserType browserType = selectBrowserType();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(properties.isHeadless())
                .setArgs(java.util.Arrays.asList(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-blink-features=AutomationControlled"));

        if (properties.getSlowMo() > 0) {
            options.setSlowMo(properties.getSlowMo());
        }

        return browserType.launch(options);
    }

    private BrowserType selectBrowserType() {
        String type = properties.getBrowserType().toLowerCase();
        return switch (type) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }

    /** 本地模式预创建 Context 池 */
    private void initializeContextPool() {
        for (int i = 0; i < properties.getPoolSize(); i++) {
            try {
                BrowserContext context = createContextOnBrowser(localBrowser, null);
                if (!contextPool.offer(context)) {
                    safeCloseContext(context);
                }
            } catch (Exception e) {
                log.warn("初始化浏览器上下文失败: {}", e.getMessage());
            }
        }
    }

    // ==================== 上下文管理 ====================

    /**
     * 在指定 Browser 上创建 BrowserContext
     */
    private BrowserContext createContextOnBrowser(Browser browser, Proxy proxy) {
        if (!browser.isConnected()) {
            throw new PlaywrightException("浏览器连接已断开，无法创建上下文");
        }

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight());

        String userAgent = properties.getUserAgent();
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";
        }
        options.setUserAgent(userAgent);
        options.setIgnoreHTTPSErrors(true);

        if (proxy != null) {
            options.setProxy(new com.microsoft.playwright.options.Proxy(proxy.toAddress()));
        }

        return browser.newContext(options);
    }

    /**
     * 获取浏览器上下文
     * <p>
     * 远程模式：从 browserQueue 取出一个独占的 Browser 连接，在其上创建 Context。
     * 本地模式：从预创建的 contextPool 中获取。
     * </p>
     *
     * @param proxy 代理（可为 null）
     * @return 浏览器上下文
     * @throws InterruptedException 如果等待被中断
     */
    public BrowserContext acquire(Proxy proxy) throws InterruptedException {
        if (shutdown.get()) {
            throw new IllegalStateException("BrowserPool 已关闭");
        }

        activeCount.incrementAndGet();

        try {
            if (properties.isRemoteMode()) {
                return acquireRemote(proxy);
            } else {
                return acquireLocal(proxy);
            }
        } catch (Exception e) {
            activeCount.decrementAndGet();
            throw e;
        }
    }

    /**
     * 远程模式获取 -- Browser 独占，每个 Context 独占一个 Browser 连接
     */
    private BrowserContext acquireRemote(Proxy proxy) throws InterruptedException {
        // 从队列取一个可用的 Browser（独占）
        Browser browser = pollAvailableBrowser();

        try {
            BrowserContext context = createContextOnBrowser(browser, proxy);
            // 记录 context -> browser 映射，release 时归还
            contextBrowserMap.put(context, browser);
            return context;
        } catch (Exception e) {
            // 创建失败，归还 Browser
            browserQueue.offer(browser);
            throw e;
        }
    }

    /**
     * 从 browserQueue 中取出一个可用的 Browser 连接
     * 如果取出的 Browser 已断开，尝试下一个或触发重连
     */
    private Browser pollAvailableBrowser() throws InterruptedException {
        int retries = 0;
        int maxRetries = allBrowserConnections.size() + 1;

        while (retries < maxRetries) {
            Browser browser = browserQueue.poll(acquireTimeoutSeconds, TimeUnit.SECONDS);
            if (browser == null) {
                throw new IllegalStateException(
                        "等待 Browser 连接超时（" + acquireTimeoutSeconds + "s），" +
                        "当前活跃: " + activeCount.get() + ", 队列剩余: " + browserQueue.size());
            }

            if (browser.isConnected()) {
                return browser;
            }

            // Browser 已断开，放弃这个连接，尝试下一个
            log.warn("取到的 Browser 已断开，尝试下一个（retry {}）", retries);
            retries++;
        }

        // 所有连接都断了，触发重连
        triggerReconnect();
        Browser browser = browserQueue.poll(acquireTimeoutSeconds, TimeUnit.SECONDS);
        if (browser == null || !browser.isConnected()) {
            throw new IllegalStateException("所有远程 Browser 连接均不可用（已尝试重连）");
        }
        return browser;
    }

    /**
     * 本地模式获取 -- 从预创建的 Context 池中取
     */
    private BrowserContext acquireLocal(Proxy proxy) throws InterruptedException {
        if (proxy != null) {
            return createContextOnBrowser(localBrowser, proxy);
        }

        int invalidCount = 0;
        BrowserContext context = contextPool.poll(30, TimeUnit.SECONDS);

        while (context != null && !isContextValid(context)) {
            invalidCount++;
            safeCloseContext(context);
            if (invalidCount >= 3) {
                break;
            }
            context = contextPool.poll(1, TimeUnit.SECONDS);
        }

        if (context == null || !isContextValid(context)) {
            if (context != null) {
                safeCloseContext(context);
            }
            log.warn("池中无可用上下文（失效 {} 个），创建新上下文", invalidCount);
            return createContextOnBrowser(localBrowser, null);
        }

        return context;
    }

    /**
     * 释放浏览器上下文
     */
    public void release(BrowserContext context) {
        release(context, false);
    }

    /**
     * 释放浏览器上下文
     *
     * @param context   上下文
     * @param corrupted 是否已损坏
     */
    public void release(BrowserContext context, boolean corrupted) {
        activeCount.decrementAndGet();

        if (context == null) {
            return;
        }

        if (properties.isRemoteMode()) {
            releaseRemote(context);
        } else {
            releaseLocal(context, corrupted);
        }
    }

    /**
     * 远程模式释放 -- 关闭 Context，归还 Browser 到队列
     */
    private void releaseRemote(BrowserContext context) {
        Browser browser = contextBrowserMap.remove(context);
        safeCloseContext(context);

        if (browser != null) {
            if (browser.isConnected()) {
                browserQueue.offer(browser);
            } else {
                log.warn("归还时 Browser 已断开，不放回队列");
            }
        }
    }

    /**
     * 本地模式释放 -- 清理后放回 Context 池
     */
    private void releaseLocal(BrowserContext context, boolean corrupted) {
        if (corrupted || !isContextValid(context)) {
            safeCloseContext(context);
            replenishLocalPool();
            return;
        }

        if (!shutdown.get() && contextPool.size() < properties.getPoolSize()) {
            try {
                context.clearCookies();
                context.pages().forEach(page -> {
                    try {
                        page.close();
                    } catch (Exception ignored) {
                    }
                });
                if (!contextPool.offer(context)) {
                    safeCloseContext(context);
                }
            } catch (Exception e) {
                log.warn("释放上下文失败: {}", e.getMessage());
                safeCloseContext(context);
                replenishLocalPool();
            }
        } else {
            safeCloseContext(context);
        }
    }

    private void safeCloseContext(BrowserContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Exception ignored) {
        }
    }

    private boolean isContextValid(BrowserContext context) {
        try {
            Browser browser = context.browser();
            return browser != null && browser.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void replenishLocalPool() {
        if (shutdown.get() || localBrowser == null || !localBrowser.isConnected()) {
            return;
        }
        try {
            if (contextPool.size() < properties.getPoolSize()) {
                BrowserContext newContext = createContextOnBrowser(localBrowser, null);
                if (!contextPool.offer(newContext)) {
                    safeCloseContext(newContext);
                }
            }
        } catch (Exception e) {
            log.warn("补充上下文失败: {}", e.getMessage());
        }
    }

    private void triggerReconnect() {
        if (!reconnectLock.tryLock()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        try {
            log.warn("所有连接不可用，主动触发重连");

            for (Map.Entry<String, List<Browser>> entry : endpointBrowsers.entrySet()) {
                String endpoint = entry.getKey();
                List<Browser> browsers = entry.getValue();

                for (int i = 0; i < browsers.size(); i++) {
                    Browser browser = browsers.get(i);
                    if (browser == null || !browser.isConnected()) {
                        browserQueue.remove(browser);
                        allBrowserConnections.remove(browser);
                        if (browser != null) {
                            try {
                                browser.close();
                            } catch (Exception ignored) {
                            }
                        }
                        Browser newBrowser = connectSingle(endpoint, i);
                        if (newBrowser != null) {
                            browsers.set(i, newBrowser);
                            allBrowserConnections.add(newBrowser);
                            browserQueue.offer(newBrowser);
                        }
                    }
                }
            }
        } finally {
            reconnectLock.unlock();
        }
    }

    // ==================== 生命周期 ====================

    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("开始关闭 BrowserPool...");

            if (healthCheckExecutor != null) {
                healthCheckExecutor.shutdown();
            }

            // 关闭本地模式池中的上下文
            BrowserContext context;
            while ((context = contextPool.poll()) != null) {
                safeCloseContext(context);
            }

            // 关闭远程模式中映射的上下文
            contextBrowserMap.keySet().forEach(this::safeCloseContext);
            contextBrowserMap.clear();

            if (localBrowser != null) {
                try {
                    localBrowser.close();
                } catch (Exception e) {
                    log.warn("关闭本地浏览器失败: {}", e.getMessage());
                }
            }

            for (Browser browser : allBrowserConnections) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.warn("关闭远程浏览器连接失败: {}", e.getMessage());
                }
            }
            allBrowserConnections.clear();
            endpointBrowsers.clear();
            browserQueue.clear();

            try {
                playwright.close();
            } catch (Exception e) {
                log.warn("关闭 Playwright 失败: {}", e.getMessage());
            }

            log.info("BrowserPool 已关闭");
        }
    }

    public boolean isHealthy() {
        return !shutdown.get() && hasAnyConnectedBrowser();
    }

    public int getAvailableCount() {
        if (properties.isRemoteMode()) {
            return browserQueue.size();
        }
        return contextPool.size();
    }

    public int getActiveCount() {
        return activeCount.get();
    }

    public PoolStats getStats() {
        int connectedBrowsers = 0;
        if (properties.isRemoteMode()) {
            for (Browser browser : allBrowserConnections) {
                if (browser != null && browser.isConnected()) {
                    connectedBrowsers++;
                }
            }
        }

        return new PoolStats(
                properties.getPoolSize(),
                getAvailableCount(),
                activeCount.get(),
                properties.getMode(),
                properties.isRemoteMode() ? properties.getRemote().getEndpoints().size() : 0,
                connectedBrowsers,
                allBrowserConnections.size(),
                browserQueue.size());
    }

    public record PoolStats(
            int totalSize,
            int availableCount,
            int activeCount,
            Mode mode,
            int totalEndpoints,
            int connectedBrowsers,
            int totalBrowserConnections,
            int availableBrowsers) {
    }
}
