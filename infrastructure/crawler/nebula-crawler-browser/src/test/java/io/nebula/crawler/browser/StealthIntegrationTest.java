package io.nebula.crawler.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.kihdev.playwright.stealth4j.Stealth4j;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stealth4j 集成测试
 * 
 * 验证 playwright-stealth-4j 能够有效隐藏浏览器自动化特征
 * 
 * 测试方法：
 * 1. 访问 bot.sannysoft.com 检测页面
 * 2. 检查 navigator.webdriver 是否被隐藏
 * 3. 检查 Chrome 运行时是否被模拟
 * 
 * 运行方式：
 * mvn test -Dtest=StealthIntegrationTest -pl nebula/infrastructure/crawler/nebula-crawler-browser
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Stealth4j 集成测试")
class StealthIntegrationTest {

    private static final boolean HEADLESS = !"false".equalsIgnoreCase(System.getenv("HEADLESS"));
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Test
    @Order(1)
    @DisplayName("对比测试：普通页面 vs Stealth 页面")
    void testStealthVsNormal() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(HEADLESS)
                    .setArgs(java.util.List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-blink-features=AutomationControlled"
                    )));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT)
                    .setViewportSize(1920, 1080));

            // 测试1: 普通页面
            System.out.println("\n========== 测试普通页面 ==========");
            Page normalPage = context.newPage();
            Object normalWebdriver = normalPage.evaluate("() => navigator.webdriver");
            Object normalChrome = normalPage.evaluate("() => typeof window.chrome !== 'undefined'");
            System.out.println("普通页面 - navigator.webdriver: " + normalWebdriver);
            System.out.println("普通页面 - window.chrome 存在: " + normalChrome);
            normalPage.close();

            // 测试2: Stealth 页面
            System.out.println("\n========== 测试 Stealth 页面 ==========");
            Page stealthPage = Stealth4j.newStealthPage(context);
            
            // 需要先导航到一个页面，让 initScript 生效
            stealthPage.navigate("about:blank");
            
            Object stealthWebdriver = stealthPage.evaluate("() => navigator.webdriver");
            Object stealthChrome = stealthPage.evaluate("() => typeof window.chrome !== 'undefined'");
            System.out.println("Stealth 页面 - navigator.webdriver: " + stealthWebdriver);
            System.out.println("Stealth 页面 - window.chrome 存在: " + stealthChrome);

            // 验证
            System.out.println("\n========== 验证结果 ==========");
            
            // 普通页面的 navigator.webdriver 通常为 true
            // Stealth 页面的 navigator.webdriver 应该为 undefined 或 false
            boolean stealthHidesWebdriver = stealthWebdriver == null || 
                    Boolean.FALSE.equals(stealthWebdriver) ||
                    "undefined".equals(String.valueOf(stealthWebdriver));
            
            System.out.println("Stealth 隐藏 webdriver: " + stealthHidesWebdriver);
            assertTrue(stealthHidesWebdriver, "Stealth 应该隐藏 navigator.webdriver");

            stealthPage.close();
            context.close();
            browser.close();
        }
    }

    @Test
    @Order(2)
    @DisplayName("验证 Stealth 页面的完整特征")
    void testStealthFeatures() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(HEADLESS));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT));

            Page page = Stealth4j.newStealthPage(context);
            page.navigate("about:blank");

            System.out.println("\n========== Stealth 特征检测 ==========");

            // 1. navigator.webdriver
            Object webdriver = page.evaluate("() => navigator.webdriver");
            System.out.println("navigator.webdriver: " + webdriver);

            // 2. window.chrome
            Object chrome = page.evaluate("() => typeof window.chrome");
            System.out.println("typeof window.chrome: " + chrome);

            // 3. chrome.runtime
            Object chromeRuntime = page.evaluate("() => typeof window.chrome?.runtime");
            System.out.println("typeof chrome.runtime: " + chromeRuntime);

            // 4. navigator.plugins
            Object pluginsLength = page.evaluate("() => navigator.plugins.length");
            System.out.println("navigator.plugins.length: " + pluginsLength);

            // 5. navigator.languages
            Object languages = page.evaluate("() => navigator.languages");
            System.out.println("navigator.languages: " + languages);

            // 6. WebGL vendor
            Object webglVendor = page.evaluate("""
                () => {
                    const canvas = document.createElement('canvas');
                    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
                    if (!gl) return 'N/A';
                    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                    return debugInfo ? gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : 'N/A';
                }
                """);
            System.out.println("WebGL vendor: " + webglVendor);

            // 验证关键特征
            assertNull(webdriver, "navigator.webdriver 应为 undefined");
            assertEquals("object", chrome, "window.chrome 应存在");

            page.close();
            context.close();
            browser.close();
        }
    }

    @Test
    @Order(3)
    @DisplayName("访问反机器人检测网站")
    @Disabled("需要网络访问，手动启用")
    void testAntiBot() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)  // 非无头模式以便查看结果
                    .setSlowMo(100));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT));

            Page page = Stealth4j.newStealthPage(context);

            System.out.println("\n========== 访问 bot.sannysoft.com ==========");
            page.navigate("https://bot.sannysoft.com/", 
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // 等待页面完全加载
            page.waitForTimeout(3000);

            // 截图保存
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("/tmp/stealth-test-result.png"))
                    .setFullPage(true));

            System.out.println("截图已保存到: /tmp/stealth-test-result.png");
            System.out.println("请检查截图中的检测结果");

            // 等待用户查看
            page.waitForTimeout(10000);

            page.close();
            context.close();
            browser.close();
        }
    }
}
