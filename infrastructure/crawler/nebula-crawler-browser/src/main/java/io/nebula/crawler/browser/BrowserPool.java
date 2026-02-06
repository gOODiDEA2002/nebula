package io.nebula.crawler.browser;

import com.microsoft.playwright.*;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties.LoadBalanceStrategy;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties.Mode;
import io.nebula.crawler.browser.util.StealthHelper;
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
 * <li>LOCAL - 本地启动浏览器实例</li>
 * <li>REMOTE - 连接到远程 Playwright Server（支持多端点负载均衡）</li>
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
    private final BlockingQueue<BrowserContext> contextPool;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /** 重连锁，防止多线程同时触发重连 */
    private final ReentrantLock reconnectLock = new ReentrantLock();

    // 本地模式使用
    private Browser localBrowser;

    // 远程模式使用
    private final Map<String, Browser> remoteBrowsers = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> endpointConnections = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private ScheduledExecutorService healthCheckExecutor;

    /**
     * 构造函数
     *
     * @param properties 浏览器配置
     */
    public BrowserPool(BrowserCrawlerProperties properties) {
        this.properties = properties;

        // 根据模式创建 Playwright 实例
        if (properties.isRemoteMode()) {
            // 远程模式：通过 CreateOptions 设置环境变量跳过浏览器下载
            log.info("远程模式启用，跳过本地浏览器下载");
            this.playwright = Playwright.create(new Playwright.CreateOptions()
                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        } else {
            // 本地模式：正常创建，允许自动下载浏览器
            this.playwright = Playwright.create();
        }

        this.contextPool = new LinkedBlockingQueue<>(properties.getPoolSize());

        // 根据模式初始化
        if (properties.isRemoteMode()) {
            initializeRemoteMode();
        } else {
            initializeLocalMode();
        }

        log.info("BrowserPool 初始化完成: mode={}, browserType={}, poolSize={}",
                properties.getMode(), properties.getBrowserType(), properties.getPoolSize());
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化本地模式
     */
    private void initializeLocalMode() {
        this.localBrowser = createLocalBrowser();
        initializeContextPool();
        log.info("本地浏览器模式初始化完成: headless={}", properties.isHeadless());
    }

    /**
     * 初始化远程模式
     */
    private void initializeRemoteMode() {
        List<String> endpoints = properties.getRemote().getEndpoints();

        if (endpoints.isEmpty()) {
            throw new IllegalStateException("远程模式需要配置至少一个端点 (nebula.crawler.browser.remote.endpoints)");
        }

        // 连接到所有端点
        for (String endpoint : endpoints) {
            connectToEndpoint(endpoint);
        }

        if (remoteBrowsers.isEmpty()) {
            throw new IllegalStateException("无法连接到任何远程端点");
        }

        // 初始化上下文池
        initializeContextPool();

        // 启动健康检查
        startHealthCheck();

        log.info("远程浏览器模式初始化完成: endpoints={}, connected={}",
                endpoints.size(), remoteBrowsers.size());
    }

    /**
     * 连接到远程端点（支持 CDP 和 Playwright Server 两种模式）
     */
    private void connectToEndpoint(String endpoint) {
        try {
            Browser browser;

            if (properties.getRemote().isUseCdp()) {
                // CDP 模式：使用 connectOverCDP 连接到 Chrome DevTools Protocol
                // 支持多客户端同时连接，比 Playwright Server 模式更稳定
                String cdpEndpoint = endpoint.replace("ws://", "http://");
                log.info("使用 CDP 模式连接到远程浏览器: {}", cdpEndpoint);

                browser = playwright.chromium().connectOverCDP(cdpEndpoint,
                        new BrowserType.ConnectOverCDPOptions()
                                .setTimeout(properties.getConnectTimeout()));

                log.info("CDP 模式连接成功: endpoint={}, connected={}", cdpEndpoint, browser.isConnected());
            } else {
                // Playwright Server 模式：使用 connect（单客户端模式）
                log.info("使用 Playwright Server 模式连接: {}", endpoint);

                browser = playwright.chromium().connect(endpoint,
                        new BrowserType.ConnectOptions()
                                .setTimeout(properties.getConnectTimeout()));

                log.info("Playwright Server 模式连接成功: endpoint={}, connected={}", endpoint, browser.isConnected());
            }

            remoteBrowsers.put(endpoint, browser);
            endpointConnections.put(endpoint, new AtomicInteger(0));

        } catch (Exception e) {
            log.error("连接远程端点失败: endpoint={}, useCdp={}, error={}",
                    endpoint, properties.getRemote().isUseCdp(), e.getMessage());
        }
    }

    /**
     * 启动健康检查
     */
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
     * 执行健康检查
     * 
     * 重连后会清理池中所有失效上下文并重新填充
     */
    private void performHealthCheck() {
        if (shutdown.get()) {
            return;
        }

        boolean reconnected = false;
        List<String> endpoints = properties.getRemote().getEndpoints();

        for (String endpoint : endpoints) {
            Browser browser = remoteBrowsers.get(endpoint);

            if (browser == null || !browser.isConnected()) {
                log.warn("端点断开连接，尝试重连: {}", endpoint);

                // 移除旧连接
                if (browser != null) {
                    try {
                        browser.close();
                    } catch (Exception ignored) {
                    }
                    remoteBrowsers.remove(endpoint);
                }

                // 尝试重连
                connectToEndpoint(endpoint);
                reconnected = true;
            }
        }

        // 重连成功后，清理失效上下文并重新填充池
        if (reconnected && hasAnyConnectedBrowser()) {
            drainAndRefillPool();
        }
    }

    /**
     * 清空池中所有失效上下文，并重新填充
     * 
     * 当远程连接断开又重连后，旧的上下文引用全部失效，
     * 必须清空并创建新的上下文
     */
    private void drainAndRefillPool() {
        if (!reconnectLock.tryLock()) {
            return;
        }
        try {
            // 清空池中所有上下文
            int drained = 0;
            BrowserContext context;
            while ((context = contextPool.poll()) != null) {
                try {
                    context.close();
                } catch (Exception ignored) {
                }
                drained++;
            }

            if (drained > 0) {
                log.info("已清空 {} 个失效上下文，开始重新填充", drained);
            }

            // 重新填充
            initializeContextPool();
            log.info("上下文池重新填充完成，当前可用: {}", contextPool.size());
        } finally {
            reconnectLock.unlock();
        }
    }

    /**
     * 检查是否有任何已连接的浏览器
     */
    private boolean hasAnyConnectedBrowser() {
        if (properties.isLocalMode()) {
            return localBrowser != null && localBrowser.isConnected();
        }
        for (Browser browser : remoteBrowsers.values()) {
            if (browser != null && browser.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建本地浏览器实例
     */
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

    /**
     * 选择浏览器类型
     */
    private BrowserType selectBrowserType() {
        String type = properties.getBrowserType().toLowerCase();
        return switch (type) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }

    /**
     * 初始化上下文池
     */
    private void initializeContextPool() {
        for (int i = 0; i < properties.getPoolSize(); i++) {
            try {
                BrowserContext context = createContext(null);
                if (!contextPool.offer(context)) {
                    try {
                        context.close();
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                log.warn("初始化浏览器上下文失败: {}", e.getMessage());
            }
        }
    }

    // ==================== 负载均衡 ====================

    /**
     * 选择浏览器（根据负载均衡策略）
     * 
     * 所有端点不可用时会主动触发重连
     */
    private Browser selectBrowser() {
        if (properties.isLocalMode()) {
            return localBrowser;
        }

        if (remoteBrowsers.isEmpty()) {
            // 尝试重连
            triggerReconnect();
            if (remoteBrowsers.isEmpty()) {
                throw new IllegalStateException("没有可用的远程浏览器连接");
            }
        }

        List<String> endpoints = properties.getRemote().getEndpoints();
        LoadBalanceStrategy strategy = properties.getRemote().getLoadBalanceStrategy();

        String selectedEndpoint = switch (strategy) {
            case RANDOM -> selectRandomEndpoint(endpoints);
            case LEAST_CONNECTIONS -> selectLeastConnectionsEndpoint(endpoints);
            default -> selectRoundRobinEndpoint(endpoints);
        };

        Browser browser = remoteBrowsers.get(selectedEndpoint);

        // 如果选中的端点不可用，尝试其他端点
        if (browser == null || !browser.isConnected()) {
            for (String endpoint : endpoints) {
                browser = remoteBrowsers.get(endpoint);
                if (browser != null && browser.isConnected()) {
                    selectedEndpoint = endpoint;
                    break;
                }
            }
        }

        // 所有端点不可用，触发重连
        if (browser == null || !browser.isConnected()) {
            triggerReconnect();
            // 重连后再尝试选择
            for (String endpoint : endpoints) {
                browser = remoteBrowsers.get(endpoint);
                if (browser != null && browser.isConnected()) {
                    selectedEndpoint = endpoint;
                    break;
                }
            }
        }

        if (browser == null || !browser.isConnected()) {
            throw new IllegalStateException("所有远程端点均不可用（已尝试重连）");
        }

        // 增加连接计数
        AtomicInteger counter = endpointConnections.get(selectedEndpoint);
        if (counter != null) {
            counter.incrementAndGet();
        }

        return browser;
    }

    /**
     * 主动触发重连（非阻塞，同一时间只允许一个线程执行重连）
     */
    private void triggerReconnect() {
        if (!reconnectLock.tryLock()) {
            // 其他线程正在重连，等待短暂时间
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        try {
            log.warn("所有端点不可用，主动触发重连");
            List<String> endpoints = properties.getRemote().getEndpoints();
            for (String endpoint : endpoints) {
                Browser browser = remoteBrowsers.get(endpoint);
                if (browser == null || !browser.isConnected()) {
                    if (browser != null) {
                        try {
                            browser.close();
                        } catch (Exception ignored) {
                        }
                        remoteBrowsers.remove(endpoint);
                    }
                    connectToEndpoint(endpoint);
                }
            }
            // 重连后清空并重新填充池
            if (hasAnyConnectedBrowser()) {
                drainAndRefillPool();
            }
        } finally {
            reconnectLock.unlock();
        }
    }

    private String selectRoundRobinEndpoint(List<String> endpoints) {
        int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % endpoints.size());
        return endpoints.get(index % endpoints.size());
    }

    private String selectRandomEndpoint(List<String> endpoints) {
        int index = ThreadLocalRandom.current().nextInt(endpoints.size());
        return endpoints.get(index);
    }

    private String selectLeastConnectionsEndpoint(List<String> endpoints) {
        String selected = endpoints.get(0);
        int minConnections = Integer.MAX_VALUE;

        for (String endpoint : endpoints) {
            Browser browser = remoteBrowsers.get(endpoint);
            if (browser != null && browser.isConnected()) {
                AtomicInteger counter = endpointConnections.get(endpoint);
                int connections = counter != null ? counter.get() : 0;
                if (connections < minConnections) {
                    minConnections = connections;
                    selected = endpoint;
                }
            }
        }

        return selected;
    }

    // ==================== 上下文管理 ====================

    /**
     * 创建浏览器上下文（带反检测配置）
     * 
     * 创建前会检查连接有效性，避免在失效连接上创建上下文
     */
    private BrowserContext createContext(Proxy proxy) {
        Browser browser = selectBrowser();

        // 前置检查：确保浏览器连接仍然有效
        if (!browser.isConnected()) {
            throw new PlaywrightException("浏览器连接已断开，无法创建上下文");
        }

        // 简化上下文配置，与 GongchangLoginTest（成功测试）保持一致
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight());

        // 设置 User-Agent（使用最新的 Chrome 版本号）
        String userAgent = properties.getUserAgent();
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";
        }
        options.setUserAgent(userAgent);

        // 忽略 HTTPS 错误
        options.setIgnoreHTTPSErrors(true);

        // 设置代理
        if (proxy != null) {
            options.setProxy(new com.microsoft.playwright.options.Proxy(proxy.toAddress()));
        }

        // 创建上下文
        BrowserContext context = browser.newContext(options);

        // 注意：不在 Context 级别注入 Stealth 脚本
        // Stealth4j 脚本由 BrowserCrawlerEngine.createPage() 在 Page 级别注入
        // 双重注入可能导致脚本冲突，被反机器人检测识别（errorCode:12）

        return context;
    }

    /**
     * 注入反检测脚本（使用 StealthHelper）
     * 
     * 注意：这里注入的是内置基础脚本，作为额外保护层。
     * 主要的反检测配置通过 Stealth4j.newStealthPage() 在页面创建时应用。
     * 
     * @see io.nebula.crawler.browser.BrowserCrawlerEngine#crawl
     */
    private void injectStealthScripts(BrowserContext context) {
        StealthHelper.applyStealthToContext(context);
    }

    /**
     * 获取浏览器上下文
     * 
     * 从池中获取时会验证上下文有效性，遇到连续失效会批量清理并重连
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

        // 如果需要代理，创建新的上下文
        if (proxy != null) {
            return createContext(proxy);
        }

        // 尝试从池中获取（带失效检测）
        int invalidCount = 0;
        BrowserContext context = contextPool.poll(30, TimeUnit.SECONDS);

        while (context != null && !isContextValid(context)) {
            invalidCount++;
            try {
                context.close();
            } catch (Exception ignored) {
            }
            // 连续 3 个以上失效，说明连接可能全断了，触发重连
            if (invalidCount >= 3) {
                log.warn("连续 {} 个上下文失效，触发重连", invalidCount);
                triggerReconnect();
                break;
            }
            // 继续尝试从池中取
            context = contextPool.poll(1, TimeUnit.SECONDS);
        }

        if (context == null || !isContextValid(context)) {
            // 池中无可用上下文，直接创建新的
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ignored) {
                }
            }
            log.warn("池中无可用上下文（失效 {} 个），创建新上下文", invalidCount);
            return createContext(null);
        }

        return context;
    }

    /**
     * 释放浏览器上下文
     *
     * @param context 上下文
     */
    public void release(BrowserContext context) {
        release(context, false);
    }

    /**
     * 释放浏览器上下文
     *
     * @param context   上下文
     * @param corrupted 是否已损坏（损坏的上下文不放回池中）
     */
    public void release(BrowserContext context, boolean corrupted) {
        activeCount.decrementAndGet();

        // 减少端点连接计数（远程模式）
        decrementEndpointConnections(context);

        if (context == null) {
            return;
        }

        // 损坏的上下文或连接已断开：直接关闭，不放回池中
        if (corrupted || !isContextValid(context)) {
            safeCloseContext(context);
            replenishPool();
            return;
        }

        if (!shutdown.get() && contextPool.size() < properties.getPoolSize()) {
            try {
                // 清理上下文状态
                context.clearCookies();

                // 关闭所有页面
                context.pages().forEach(page -> {
                    try {
                        page.close();
                    } catch (Exception ignored) {
                    }
                });

                // 放回池中
                if (!contextPool.offer(context)) {
                    safeCloseContext(context);
                }
            } catch (Exception e) {
                log.warn("释放上下文失败: {}", e.getMessage());
                safeCloseContext(context);
                replenishPool();
            }
        } else {
            safeCloseContext(context);
        }
    }

    /**
     * 安全关闭上下文（忽略异常）
     */
    private void safeCloseContext(BrowserContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 减少端点连接计数
     */
    private void decrementEndpointConnections(BrowserContext context) {
        if (!properties.isRemoteMode() || context == null) {
            return;
        }
        try {
            Browser browser = context.browser();
            for (Map.Entry<String, Browser> entry : remoteBrowsers.entrySet()) {
                if (entry.getValue() == browser) {
                    AtomicInteger counter = endpointConnections.get(entry.getKey());
                    if (counter != null) {
                        counter.decrementAndGet();
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 检查上下文是否仍然有效
     */
    private boolean isContextValid(BrowserContext context) {
        try {
            Browser browser = context.browser();
            return browser != null && browser.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 补充池中的上下文
     * 
     * 创建前检查是否有可用的浏览器连接
     */
    private void replenishPool() {
        if (shutdown.get()) {
            return;
        }

        // 检查是否有可用的浏览器连接
        if (!hasAnyConnectedBrowser()) {
            log.debug("无可用浏览器连接，跳过补充上下文");
            return;
        }

        try {
            if (contextPool.size() < properties.getPoolSize()) {
                BrowserContext newContext = createContext(null);
                if (!contextPool.offer(newContext)) {
                    safeCloseContext(newContext);
                }
            }
        } catch (Exception e) {
            log.warn("补充上下文失败: {}", e.getMessage());
        }
    }

    /**
     * 关闭浏览器池
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("开始关闭 BrowserPool...");

            // 停止健康检查
            if (healthCheckExecutor != null) {
                healthCheckExecutor.shutdown();
            }

            // 关闭池中的上下文
            BrowserContext context;
            while ((context = contextPool.poll()) != null) {
                safeCloseContext(context);
            }

            // 关闭本地浏览器
            if (localBrowser != null) {
                try {
                    localBrowser.close();
                } catch (Exception e) {
                    log.warn("关闭本地浏览器失败: {}", e.getMessage());
                }
            }

            // 关闭远程连接
            for (Browser browser : remoteBrowsers.values()) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.warn("关闭远程浏览器连接失败: {}", e.getMessage());
                }
            }
            remoteBrowsers.clear();

            // 关闭 Playwright
            try {
                playwright.close();
            } catch (Exception e) {
                log.warn("关闭 Playwright 失败: {}", e.getMessage());
            }

            log.info("BrowserPool 已关闭");
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        if (shutdown.get()) {
            return false;
        }

        return hasAnyConnectedBrowser();
    }

    /**
     * 获取池中可用上下文数量
     */
    public int getAvailableCount() {
        return contextPool.size();
    }

    /**
     * 获取正在使用的上下文数量
     */
    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 获取池统计信息
     */
    public PoolStats getStats() {
        int connectedEndpoints = 0;
        if (properties.isRemoteMode()) {
            for (Browser browser : remoteBrowsers.values()) {
                if (browser.isConnected()) {
                    connectedEndpoints++;
                }
            }
        }

        return new PoolStats(
                properties.getPoolSize(),
                contextPool.size(),
                activeCount.get(),
                properties.getMode(),
                properties.isRemoteMode() ? properties.getRemote().getEndpoints().size() : 0,
                connectedEndpoints);
    }

    /**
     * 池统计信息
     */
    public record PoolStats(
            int totalSize,
            int availableCount,
            int activeCount,
            Mode mode,
            int totalEndpoints,
            int connectedEndpoints) {
    }
}
