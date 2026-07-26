package io.nebula.crawler.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import io.nebula.crawler.browser.config.BrowserCrawlerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 远端浏览器资源回收行为测试。
 *
 * <p>动机（生产事故）：上下文关闭失败时若静默吞掉异常并把连接照常放回队列，远端页面/上下文不会被
 * 回收，长跑下浏览器进程持续堆积（实测单容器堆到 698 个 chrome 进程）直至连接耗尽、采集停滞。
 * 本测试钉住两条不变量：关闭失败必须被识别；识别后该连接不得原样复用。
 */
@DisplayName("远端浏览器资源回收")
class BrowserResourceReclaimTest {

    /** 构造一个只做远程释放路径的池（不触发真实连接） */
    private BrowserPool remotePool(Browser browser, BrowserContext context, String endpoint) {
        BrowserPool pool = BrowserPoolFactory.raw();
        // 远程释放路径依赖的几个结构，逐个填充
        Map<BrowserContext, Browser> ctxMap = new ConcurrentHashMap<>();
        ctxMap.put(context, browser);
        ReflectionTestUtils.setField(pool, "contextBrowserMap", ctxMap);

        BlockingQueue<Browser> queue = new LinkedBlockingQueue<>();
        ReflectionTestUtils.setField(pool, "browserQueue", queue);

        List<Browser> conns = new CopyOnWriteArrayList<>();
        conns.add(browser);
        ReflectionTestUtils.setField(pool, "allBrowserConnections", conns);

        Map<String, List<Browser>> byEndpoint = new ConcurrentHashMap<>();
        byEndpoint.put(endpoint, new CopyOnWriteArrayList<>(List.of(browser)));
        ReflectionTestUtils.setField(pool, "endpointBrowsers", byEndpoint);

        BrowserCrawlerProperties props = new BrowserCrawlerProperties();
        ReflectionTestUtils.setField(pool, "properties", props);
        return pool;
    }

    @Test
    @DisplayName("上下文正常关闭：连接放回队列复用")
    void healthyContextReturnsBrowserToQueue() {
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        when(browser.isConnected()).thenReturn(true);

        BrowserPool pool = remotePool(browser, context, "ws://stub:9222");
        ReflectionTestUtils.invokeMethod(pool, "releaseRemote", context);

        verify(context).close();
        @SuppressWarnings("unchecked")
        BlockingQueue<Browser> queue =
                (BlockingQueue<Browser>) ReflectionTestUtils.getField(pool, "browserQueue");
        assertEquals(1, queue.size(), "健康连接应放回队列复用");
        assertSame(browser, queue.peek());
        verify(browser, never()).close();
    }

    @Test
    @DisplayName("上下文关闭失败：连接不得原样复用（否则远端残留会不断累积）")
    void failedContextCloseDiscardsBrowser() {
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        when(browser.isConnected()).thenReturn(true);
        doThrow(new RuntimeException("Target closed")).when(context).close();

        BrowserPool pool = remotePool(browser, context, "ws://stub:9222");
        ReflectionTestUtils.invokeMethod(pool, "releaseRemote", context);

        // 该连接被弃用：关闭且从登记结构摘除；重建因端点不可达而失败，队列保持为空
        verify(browser).close();
        @SuppressWarnings("unchecked")
        BlockingQueue<Browser> queue =
                (BlockingQueue<Browser>) ReflectionTestUtils.getField(pool, "browserQueue");
        assertFalse(queue.contains(browser), "关闭失败的连接不得放回队列");

        @SuppressWarnings("unchecked")
        List<Browser> conns =
                (List<Browser>) ReflectionTestUtils.getField(pool, "allBrowserConnections");
        assertFalse(conns.contains(browser), "弃用的连接应从登记结构摘除");
    }

    @Test
    @DisplayName("safeCloseContext 如实返回关闭结果，不再静默吞异常")
    void safeCloseReportsOutcome() {
        Browser browser = mock(Browser.class);
        BrowserContext ok = mock(BrowserContext.class);
        BrowserContext bad = mock(BrowserContext.class);
        doThrow(new RuntimeException("boom")).when(bad).close();

        BrowserPool pool = remotePool(browser, ok, "ws://stub:9222");

        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pool, "safeCloseContext", ok)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pool, "safeCloseContext", bad)));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pool, "safeCloseContext", (Object) null)),
                "空上下文视为已关闭");
    }

    /** 绕开构造函数拿到未初始化的 BrowserPool 实例（构造会立即连接真实浏览器，测试不可触发） */
    static final class BrowserPoolFactory {
        static BrowserPool raw() {
            return new org.objenesis.ObjenesisStd().newInstance(BrowserPool.class);
        }
    }
}
