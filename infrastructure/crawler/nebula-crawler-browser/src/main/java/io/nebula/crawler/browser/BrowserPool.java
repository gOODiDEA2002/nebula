package io.nebula.crawler.browser;

import com.microsoft.playwright.*;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties.LoadBalanceStrategy;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties.Mode;
import io.nebula.crawler.core.proxy.Proxy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器实例池
 * <p>
 * 管理 Playwright 浏览器实例的创建和复用，支持两种模式：
 * <ul>
 *   <li>LOCAL - 本地启动浏览器实例</li>
 *   <li>REMOTE - 连接到远程 Playwright Server（支持多端点负载均衡）</li>
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
        this.playwright = Playwright.create();
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
     * 连接到远程端点
     */
    private void connectToEndpoint(String endpoint) {
        try {
            log.info("连接到远程 Playwright Server: {}", endpoint);
            
            Browser browser = playwright.chromium().connect(endpoint,
                new BrowserType.ConnectOptions()
                    .setTimeout(properties.getConnectTimeout()));
            
            remoteBrowsers.put(endpoint, browser);
            endpointConnections.put(endpoint, new AtomicInteger(0));
            
            log.info("远程端点连接成功: endpoint={}, connected={}", endpoint, browser.isConnected());
            
        } catch (Exception e) {
            log.error("连接远程端点失败: endpoint={}, error={}", endpoint, e.getMessage());
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
     */
    private void performHealthCheck() {
        if (shutdown.get()) {
            return;
        }
        
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
            }
        }
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
                "--disable-blink-features=AutomationControlled"
            ));
        
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
                contextPool.offer(context);
            } catch (Exception e) {
                log.warn("初始化浏览器上下文失败: {}", e.getMessage());
            }
        }
    }
    
    // ==================== 负载均衡 ====================
    
    /**
     * 选择浏览器（根据负载均衡策略）
     */
    private Browser selectBrowser() {
        if (properties.isLocalMode()) {
            return localBrowser;
        }
        
        if (remoteBrowsers.isEmpty()) {
            throw new IllegalStateException("没有可用的远程浏览器连接");
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
        
        if (browser == null || !browser.isConnected()) {
            throw new IllegalStateException("所有远程端点均不可用");
        }
        
        // 增加连接计数
        AtomicInteger counter = endpointConnections.get(selectedEndpoint);
        if (counter != null) {
            counter.incrementAndGet();
        }
        
        return browser;
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
     */
    private BrowserContext createContext(Proxy proxy) {
        Browser browser = selectBrowser();
        
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight());
        
        // 设置 User-Agent
        String userAgent = properties.getUserAgent();
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        }
        options.setUserAgent(userAgent);
        
        // 设置语言和时区（反检测关键）
        options.setLocale("zh-CN");
        options.setTimezoneId("Asia/Shanghai");
        
        // 设置设备参数
        options.setDeviceScaleFactor(1.0);
        options.setHasTouch(false);
        options.setIsMobile(false);
        
        // 设置颜色方案
        options.setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT);
        
        // 忽略 HTTPS 错误
        options.setIgnoreHTTPSErrors(true);
        
        // 设置代理
        if (proxy != null) {
            options.setProxy(new com.microsoft.playwright.options.Proxy(proxy.toAddress()));
        }
        
        // 创建上下文
        BrowserContext context = browser.newContext(options);
        
        // 注入反检测脚本
        injectStealthScripts(context);
        
        return context;
    }
    
    /**
     * 注入反检测脚本
     */
    private void injectStealthScripts(BrowserContext context) {
        try {
            context.addInitScript("""
                // 移除 webdriver 标记
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
                
                // 模拟真实的 plugins
                Object.defineProperty(navigator, 'plugins', {
                    get: () => {
                        const plugins = [
                            {
                                name: 'Chrome PDF Plugin',
                                description: 'Portable Document Format',
                                filename: 'internal-pdf-viewer',
                                length: 1
                            },
                            {
                                name: 'Chrome PDF Viewer',
                                description: '',
                                filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
                                length: 1
                            },
                            {
                                name: 'Native Client',
                                description: '',
                                filename: 'internal-nacl-plugin',
                                length: 2
                            }
                        ];
                        plugins.item = (index) => plugins[index];
                        plugins.namedItem = (name) => plugins.find(p => p.name === name);
                        plugins.refresh = () => {};
                        return plugins;
                    }
                });
                
                // 模拟真实的 languages
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en-US', 'en']
                });
                
                // 模拟真实的 platform
                Object.defineProperty(navigator, 'platform', {
                    get: () => 'MacIntel'
                });
                
                // 模拟真实的 hardwareConcurrency
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => 8
                });
                
                // 模拟真实的 deviceMemory
                Object.defineProperty(navigator, 'deviceMemory', {
                    get: () => 8
                });
                
                // 隐藏自动化相关属性
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;
                
                // 重写 chrome 相关对象
                window.chrome = {
                    runtime: {},
                    loadTimes: function() {},
                    csi: function() {},
                    app: {}
                };
                
                // 模拟权限 API
                const originalQuery = window.navigator.permissions.query;
                window.navigator.permissions.query = (parameters) => (
                    parameters.name === 'notifications' ?
                        Promise.resolve({ state: Notification.permission }) :
                        originalQuery(parameters)
                );
                """);
            
            log.debug("反检测脚本注入成功");
        } catch (Exception e) {
            log.warn("反检测脚本注入失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取浏览器上下文
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
        
        // 尝试从池中获取
        BrowserContext context = contextPool.poll(30, TimeUnit.SECONDS);
        
        if (context == null) {
            log.warn("从池中获取上下文超时，创建临时上下文");
            return createContext(null);
        }
        
        // 验证上下文是否仍然有效
        if (!isContextValid(context)) {
            log.debug("上下文已失效，创建新上下文");
            try {
                context.close();
            } catch (Exception ignored) {
            }
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
        if (properties.isRemoteMode() && context != null) {
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
        
        if (context == null) {
            return;
        }
        
        // 损坏的上下文直接关闭，不放回池中
        if (corrupted) {
            try {
                context.close();
            } catch (Exception ignored) {
            }
            replenishPool();
            return;
        }
        
        if (!shutdown.get() && contextPool.size() < properties.getPoolSize()) {
            try {
                if (!isContextValid(context)) {
                    log.debug("上下文已失效，关闭并补充");
                    context.close();
                    replenishPool();
                    return;
                }
                
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
                    context.close();
                }
            } catch (Exception e) {
                log.warn("释放上下文失败: {}", e.getMessage());
                try {
                    context.close();
                } catch (Exception ignored) {
                }
                replenishPool();
            }
        } else {
            try {
                context.close();
            } catch (Exception ignored) {
            }
        }
    }
    
    /**
     * 检查上下文是否仍然有效
     */
    private boolean isContextValid(BrowserContext context) {
        try {
            return context.browser().isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 补充池中的上下文
     */
    private void replenishPool() {
        if (shutdown.get()) {
            return;
        }
        
        // 检查是否有可用的浏览器
        boolean hasAvailableBrowser = properties.isLocalMode() 
            ? (localBrowser != null && localBrowser.isConnected())
            : !remoteBrowsers.isEmpty();
        
        if (!hasAvailableBrowser) {
            return;
        }
        
        try {
            if (contextPool.size() < properties.getPoolSize()) {
                BrowserContext newContext = createContext(null);
                if (!contextPool.offer(newContext)) {
                    newContext.close();
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
                try {
                    context.close();
                } catch (Exception ignored) {
                }
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
        
        if (properties.isLocalMode()) {
            return localBrowser != null && localBrowser.isConnected();
        } else {
            // 远程模式：至少有一个端点可用
            for (Browser browser : remoteBrowsers.values()) {
                if (browser.isConnected()) {
                    return true;
                }
            }
            return false;
        }
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
            connectedEndpoints
        );
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
        int connectedEndpoints
    ) {
    }
}
