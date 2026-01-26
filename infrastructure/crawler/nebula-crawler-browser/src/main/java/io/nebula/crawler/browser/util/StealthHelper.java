package io.nebula.crawler.browser.util;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.github.kihdev.playwright.stealth4j.Stealth4j;
import io.github.kihdev.playwright.stealth4j.Stealth4jConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 浏览器隐身辅助工具
 * 
 * 用于隐藏 Playwright 的自动化特征，绕过反机器人检测
 * 
 * 推荐使用方式：
 * 1. 创建页面时使用 {@link #createStealthPage(BrowserContext)} 或直接使用 {@link Stealth4j#newStealthPage}
 * 2. 不推荐在 context 级别应用（因为 stealth 脚本需要在每个页面上单独注入）
 * 
 * @author Nebula Team
 * @since 2.0.2
 */
@Slf4j
public class StealthHelper {

    private static final Stealth4jConfig DEFAULT_CONFIG = Stealth4jConfig.builder()
            .navigatorLanguages(true, java.util.List.of("zh-CN", "zh", "en-US", "en"))
            .navigatorHardwareConcurrency(true, 8)
            .build();

    /**
     * 创建带反检测配置的页面（推荐方式）
     * 
     * @param context 浏览器上下文
     * @return 带反检测配置的页面
     */
    public static Page createStealthPage(BrowserContext context) {
        return createStealthPage(context, DEFAULT_CONFIG);
    }

    /**
     * 创建带反检测配置的页面（自定义配置）
     * 
     * @param context 浏览器上下文
     * @param config  Stealth4j 配置
     * @return 带反检测配置的页面
     */
    public static Page createStealthPage(BrowserContext context, Stealth4jConfig config) {
        try {
            Page page = Stealth4j.newStealthPage(context, config);
            log.debug("使用 Stealth4j 创建反检测页面");
            return page;
        } catch (Exception e) {
            log.warn("Stealth4j 创建页面失败，使用内置脚本: {}", e.getMessage());
            Page page = context.newPage();
            applyBuiltinStealth(page);
            return page;
        }
    }

    /**
     * 对上下文应用反检测配置（备用方式）
     * 
     * 注意：此方法会为后续创建的所有页面自动应用反检测配置，
     * 但对于已存在的页面无效。推荐使用 {@link #createStealthPage} 替代。
     * 
     * @param context 浏览器上下文
     */
    public static void applyStealthToContext(BrowserContext context) {
        // 使用内置脚本作为基础保护（对已有页面生效）
        applyBuiltinStealth(context);
    }

    /**
     * 对上下文应用内置隐身脚本
     */
    public static void applyBuiltinStealth(BrowserContext context) {
        try {
            context.addInitScript(getStealthScript());
            log.debug("内置隐身脚本注入到上下文成功");
        } catch (Exception e) {
            log.warn("内置隐身脚本注入到上下文失败: {}", e.getMessage());
        }
    }

    /**
     * 对页面应用内置隐身脚本
     */
    public static void applyBuiltinStealth(Page page) {
        try {
            page.addInitScript(getStealthScript());
            log.debug("内置隐身脚本注入到页面成功");
        } catch (Exception e) {
            log.warn("内置隐身脚本注入到页面失败: {}", e.getMessage());
        }
    }

    /**
     * 获取隐身脚本
     */
    public static String getStealthScript() {
        return """
            // ========== Playwright Stealth Script ==========
            
            // 1. 移除 webdriver 标记（最关键）
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined,
                configurable: true
            });
            
            // 2. 隐藏 Playwright 和 Puppeteer 标记
            delete window.__playwright;
            delete window.__pw_manual;
            delete window.__PW_inspect;
            
            // 3. 模拟真实的 Chrome 对象
            if (!window.chrome) {
                window.chrome = {
                    runtime: {
                        id: undefined,
                        connect: () => {},
                        sendMessage: () => {},
                        onMessage: { addListener: () => {} }
                    },
                    loadTimes: () => ({
                        requestTime: Date.now() / 1000,
                        startLoadTime: Date.now() / 1000,
                        commitLoadTime: Date.now() / 1000,
                        finishDocumentLoadTime: Date.now() / 1000,
                        finishLoadTime: Date.now() / 1000,
                        firstPaintTime: Date.now() / 1000,
                        firstPaintAfterLoadTime: 0,
                        navigationType: 'Other'
                    }),
                    csi: () => ({
                        startE: Date.now(),
                        onloadT: Date.now(),
                        pageT: 0
                    }),
                    app: {
                        isInstalled: false,
                        InstallState: { DISABLED: 'disabled', INSTALLED: 'installed', NOT_INSTALLED: 'not_installed' },
                        RunningState: { CANNOT_RUN: 'cannot_run', READY_TO_RUN: 'ready_to_run', RUNNING: 'running' }
                    }
                };
            }
            
            // 4. 模拟真实的 plugins
            Object.defineProperty(navigator, 'plugins', {
                get: () => {
                    const plugins = [
                        {
                            name: 'Chrome PDF Plugin',
                            description: 'Portable Document Format',
                            filename: 'internal-pdf-viewer',
                            length: 1,
                            0: { type: 'application/x-google-chrome-pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        },
                        {
                            name: 'Chrome PDF Viewer',
                            description: '',
                            filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
                            length: 1,
                            0: { type: 'application/pdf', suffixes: 'pdf', description: '' }
                        },
                        {
                            name: 'Native Client',
                            description: '',
                            filename: 'internal-nacl-plugin',
                            length: 2,
                            0: { type: 'application/x-nacl', suffixes: '', description: 'Native Client Executable' },
                            1: { type: 'application/x-pnacl', suffixes: '', description: 'Portable Native Client Executable' }
                        }
                    ];
                    plugins.item = (index) => plugins[index];
                    plugins.namedItem = (name) => plugins.find(p => p.name === name);
                    plugins.refresh = () => {};
                    Object.setPrototypeOf(plugins, PluginArray.prototype);
                    return plugins;
                },
                configurable: true
            });
            
            // 5. 模拟真实的 mimeTypes
            Object.defineProperty(navigator, 'mimeTypes', {
                get: () => {
                    const mimeTypes = [
                        { type: 'application/pdf', suffixes: 'pdf', description: '' },
                        { type: 'application/x-google-chrome-pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                        { type: 'application/x-nacl', suffixes: '', description: 'Native Client Executable' },
                        { type: 'application/x-pnacl', suffixes: '', description: 'Portable Native Client Executable' }
                    ];
                    mimeTypes.item = (index) => mimeTypes[index];
                    mimeTypes.namedItem = (name) => mimeTypes.find(m => m.type === name);
                    Object.setPrototypeOf(mimeTypes, MimeTypeArray.prototype);
                    return mimeTypes;
                },
                configurable: true
            });
            
            // 6. 模拟真实的 languages
            Object.defineProperty(navigator, 'languages', {
                get: () => ['zh-CN', 'zh', 'en-US', 'en'],
                configurable: true
            });
            
            // 7. 隐藏自动化特征
            Object.defineProperty(navigator, 'platform', {
                get: () => 'MacIntel',
                configurable: true
            });
            
            Object.defineProperty(navigator, 'hardwareConcurrency', {
                get: () => 8,
                configurable: true
            });
            
            Object.defineProperty(navigator, 'deviceMemory', {
                get: () => 8,
                configurable: true
            });
            
            Object.defineProperty(navigator, 'maxTouchPoints', {
                get: () => 0,
                configurable: true
            });
            
            // 8. 隐藏 Chromium 自动化变量
            const automationVars = [
                'cdc_adoQpoasnfa76pfcZLmcfl_Array',
                'cdc_adoQpoasnfa76pfcZLmcfl_Promise',
                'cdc_adoQpoasnfa76pfcZLmcfl_Symbol',
                'cdc_adoQpoasnfa76pfcZLmcfl_Object',
                'cdc_adoQpoasnfa76pfcZLmcfl_proxy'
            ];
            automationVars.forEach(v => {
                try { delete window[v]; } catch(e) {}
            });
            
            // 9. 模拟权限 API
            const originalQuery = navigator.permissions.query;
            navigator.permissions.query = (parameters) => (
                parameters.name === 'notifications' ?
                    Promise.resolve({ state: Notification.permission }) :
                    originalQuery.call(navigator.permissions, parameters)
            );
            
            // 10. 修复 iframe contentWindow 检测
            const originalContentWindowGetter = Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype, 'contentWindow').get;
            Object.defineProperty(HTMLIFrameElement.prototype, 'contentWindow', {
                get: function() {
                    const contentWindow = originalContentWindowGetter.call(this);
                    if (contentWindow) {
                        try {
                            Object.defineProperty(contentWindow.navigator, 'webdriver', {
                                get: () => undefined
                            });
                        } catch(e) {}
                    }
                    return contentWindow;
                }
            });
            
            // 11. 隐藏 console.debug 自动化日志
            const originalConsoleDebug = console.debug;
            console.debug = function(...args) {
                const message = args.join(' ');
                if (message.includes('webdriver') || message.includes('automation')) {
                    return;
                }
                return originalConsoleDebug.apply(console, args);
            };
            """;
    }

    /**
     * 对单个页面应用隐身配置
     */
    public static void applyStealthToPage(Page page) {
        try {
            page.addInitScript(getStealthScript());
        } catch (Exception e) {
            log.warn("页面隐身脚本注入失败: {}", e.getMessage());
        }
    }
}
