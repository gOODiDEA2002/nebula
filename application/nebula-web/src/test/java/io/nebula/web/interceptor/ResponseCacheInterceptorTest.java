package io.nebula.web.interceptor;

import io.nebula.web.autoconfigure.WebProperties;
import io.nebula.web.cache.CacheKeyGenerator;
import io.nebula.web.cache.ResponseCache;
import io.nebula.web.cache.ResponseCacheable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证响应缓存的可缓存判定：仅缓存显式标注 @ResponseCacheable 且不带认证凭据的 GET 请求，
 * 修复此前"所有 GET 自动缓存 + 缓存键不含身份"导致的跨用户串号。
 */
class ResponseCacheInterceptorTest {

    private ResponseCacheInterceptor interceptor;
    private HandlerMethod cacheableHandler;
    private HandlerMethod plainHandler;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new ResponseCacheInterceptor(
                mock(ResponseCache.class), mock(CacheKeyGenerator.class), new WebProperties.Cache());
        TestController controller = new TestController();
        cacheableHandler = new HandlerMethod(controller, TestController.class.getMethod("publicData"));
        plainHandler = new HandlerMethod(controller, TestController.class.getMethod("userData"));
    }

    @Test
    void cacheableWhenAnnotatedGetWithoutCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        assertThat(interceptor.isCacheable(request, cacheableHandler)).isTrue();
    }

    @Test
    void notCacheableWithAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        request.addHeader("Authorization", "Bearer token");
        assertThat(interceptor.isCacheable(request, cacheableHandler)).isFalse();
    }

    @Test
    void notCacheableWithCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        request.addHeader("Cookie", "SESSION=abc");
        assertThat(interceptor.isCacheable(request, cacheableHandler)).isFalse();
    }

    @Test
    void notCacheableWithoutAnnotation() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user");
        assertThat(interceptor.isCacheable(request, plainHandler)).isFalse();
    }

    @Test
    void notCacheableForNonGet() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/public");
        assertThat(interceptor.isCacheable(request, cacheableHandler)).isFalse();
    }

    static class TestController {
        @ResponseCacheable
        public String publicData() {
            return "public";
        }

        public String userData() {
            return "user";
        }
    }
}
