package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HttpRpcClient 单元测试
 * 
 * 测试覆盖场景：
 * 1. 正常调用 - 无需类型转换
 * 2. 正常调用 - 需要类型转换（LinkedHashMap -> DTO）
 * 3. 正常调用 - 方法返回类型查找（带参数）
 * 4. 正常调用 - 方法返回类型查找（无参数）
 * 5. 调用失败 - 服务端返回错误
 * 6. 调用失败 - 类型转换异常
 * 7. 调用失败 - 网络异常
 * 8. 缓存验证 - 方法反射缓存生效
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRpcClient 单元测试")
class HttpRpcClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private Executor executor;
    private HttpRpcClient httpRpcClient;

    // 测试用的服务接口
    interface TestService {
        String echo(String message);
        TestDto getTestDto(Long id);
        void noReturn();
    }

    // 测试用的 DTO
    static class TestDto {
        private Long id;
        private String name;

        public TestDto() {}

        public TestDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDto testDto = (TestDto) o;
            return id.equals(testDto.id) && name.equals(testDto.name);
        }

        @Override
        public int hashCode() {
            return id.hashCode() + name.hashCode();
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = Executors.newFixedThreadPool(2);
        httpRpcClient = new HttpRpcClient(restTemplate, "http://localhost:8080", executor, objectMapper);
    }

    /**
     * 设置 RestTemplate Mock 返回指定的 RpcResponse
     */
    private void mockRestTemplateResponse(RpcResponse response) {
        ResponseEntity<RpcResponse> responseEntity = ResponseEntity.ok(response);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(RpcResponse.class)
        )).thenReturn(responseEntity);
    }

    @Nested
    @DisplayName("正常调用场景")
    class SuccessfulCallTests {

        @Test
        @DisplayName("正常调用 - 无需类型转换")
        @SuppressWarnings("unchecked")
        void testSuccessfulCallWithoutConversion() {
            // Arrange
            RpcResponse response = RpcResponse.success("test-request-id", "Echo: Hello");
            mockRestTemplateResponse(response);

            // Act
            Object result = httpRpcClient.call(TestService.class, "echo", "Hello");

            // Assert
            assertEquals("Echo: Hello", result);
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            );
        }

        @Test
        @DisplayName("正常调用 - 需要类型转换（LinkedHashMap -> DTO）")
        @SuppressWarnings("unchecked")
        void testSuccessfulCallWithConversion() {
            // Arrange
            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("id", 1);  // Jackson 会将整数反序列化为 Integer
            rawData.put("name", "Test User");

            RpcResponse response = RpcResponse.success("test-request-id", rawData);
            mockRestTemplateResponse(response);

            // Act
            Object result = httpRpcClient.call(TestService.class, "getTestDto", 1L);

            // Assert
            assertNotNull(result);
            assertTrue(result instanceof TestDto);
            TestDto dto = (TestDto) result;
            assertEquals(1L, dto.getId());
            assertEquals("Test User", dto.getName());
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            );
        }

        @Test
        @DisplayName("正常调用 - 方法返回类型查找（带参数）")
        @SuppressWarnings("unchecked")
        void testMethodReturnTypeLookupWithParameters() {
            // Arrange
            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("id", 2);
            rawData.put("name", "Another User");

            RpcResponse response = RpcResponse.success("test-request-id", rawData);
            mockRestTemplateResponse(response);

            // Act
            Object result = httpRpcClient.call(TestService.class, "getTestDto", 2L);

            // Assert
            assertNotNull(result);
            assertTrue(result instanceof TestDto);
            TestDto dto = (TestDto) result;
            assertEquals(2L, dto.getId());
            assertEquals("Another User", dto.getName());
        }

        @Test
        @DisplayName("正常调用 - 方法返回类型查找（无参数）")
        @SuppressWarnings("unchecked")
        void testMethodReturnTypeLookupWithoutParameters() {
            // Arrange
            RpcResponse response = RpcResponse.success("test-request-id", null);
            mockRestTemplateResponse(response);

            // Act
            httpRpcClient.call(TestService.class, "noReturn");

            // Assert
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            );
        }
    }

    @Nested
    @DisplayName("调用失败场景")
    class FailureCallTests {

        @Test
        @DisplayName("调用失败 - 服务端返回错误")
        @SuppressWarnings("unchecked")
        void testFailedCallWithServerError() {
            // Arrange
            RpcResponse response = RpcResponse.error("test-request-id", "服务内部错误");
            mockRestTemplateResponse(response);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                httpRpcClient.call(TestService.class, "echo", "Hello");
            });

            assertTrue(exception.getMessage().contains("RPC调用失败"));
            assertTrue(exception.getMessage().contains("服务内部错误"));
        }

        @Test
        @DisplayName("调用失败 - 类型转换异常")
        @SuppressWarnings("unchecked")
        void testFailedCallWithTypeConversionError() {
            // Arrange - 返回不兼容的数据类型
            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("id", "invalid_id");  // String 而不是 Long
            rawData.put("name", "Test User");

            RpcResponse response = RpcResponse.success("test-request-id", rawData);
            mockRestTemplateResponse(response);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                httpRpcClient.call(TestService.class, "getTestDto", 1L);
            });

            assertTrue(exception.getMessage().contains("RPC响应类型转换失败"));
            assertTrue(exception.getMessage().contains("TestService"));
            assertTrue(exception.getMessage().contains("getTestDto"));
        }

        @Test
        @DisplayName("调用失败 - 网络异常")
        @SuppressWarnings("unchecked")
        void testFailedCallWithNetworkError() {
            // Arrange
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            )).thenThrow(new RestClientException("Connection timeout"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                httpRpcClient.call(TestService.class, "echo", "Hello");
            });

            // 网络异常在 sendRequest 中被捕获并包装为 RPC调用失败
            assertTrue(exception.getMessage().contains("RPC调用失败") || exception.getMessage().contains("RPC调用异常"));
            assertTrue(exception.getMessage().contains("Connection timeout"));
        }
    }

    @Nested
    @DisplayName("性能优化场景")
    class PerformanceTests {

        @Test
        @DisplayName("缓存验证 - 方法反射缓存生效")
        @SuppressWarnings("unchecked")
        void testMethodReflectionCache() throws NoSuchFieldException, IllegalAccessException {
            // Arrange
            RpcResponse response1 = RpcResponse.success("test-request-id-1", "Echo: First");
            RpcResponse response2 = RpcResponse.success("test-request-id-2", "Echo: Second");

            // Mock two consecutive calls
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            ))
                    .thenReturn(ResponseEntity.ok(response1))
                    .thenReturn(ResponseEntity.ok(response2));

            // Act - 调用两次相同的方法
            Object result1 = httpRpcClient.call(TestService.class, "echo", "First");
            Object result2 = httpRpcClient.call(TestService.class, "echo", "Second");

            // Assert
            assertEquals("Echo: First", result1);
            assertEquals("Echo: Second", result2);

            // Verify - 检查缓存是否有效（访问私有字段 methodCache）
            Field methodCacheField = HttpRpcClient.class.getDeclaredField("methodCache");
            methodCacheField.setAccessible(true);
            Map<?, ?> methodCache = (Map<?, ?>) methodCacheField.get(httpRpcClient);

            // 两次调用应该使用同一个缓存的 Method 对象
            assertEquals(1, methodCache.size(), "方法应该被缓存，缓存中应该只有 1 个条目");
        }

        @Test
        @DisplayName("缓存验证 - 不同方法分别缓存")
        @SuppressWarnings("unchecked")
        void testMethodReflectionCacheForDifferentMethods() throws NoSuchFieldException, IllegalAccessException {
            // Arrange
            RpcResponse response1 = RpcResponse.success("test-request-id-1", "Echo: Hello");
            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("id", 1);
            rawData.put("name", "Test User");
            RpcResponse response2 = RpcResponse.success("test-request-id-2", rawData);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            ))
                    .thenReturn(ResponseEntity.ok(response1))
                    .thenReturn(ResponseEntity.ok(response2));

            // Act - 调用两个不同的方法
            Object result1 = httpRpcClient.call(TestService.class, "echo", "Hello");
            Object result2 = httpRpcClient.call(TestService.class, "getTestDto", 1L);

            // Assert
            assertEquals("Echo: Hello", result1);
            assertTrue(result2 instanceof TestDto);
            TestDto dto = (TestDto) result2;
            assertEquals(1L, dto.getId());

            // Verify - 检查缓存（访问私有字段 methodCache）
            Field methodCacheField = HttpRpcClient.class.getDeclaredField("methodCache");
            methodCacheField.setAccessible(true);
            Map<?, ?> methodCache = (Map<?, ?>) methodCacheField.get(httpRpcClient);

            // 两个不同的方法应该分别被缓存
            assertEquals(2, methodCache.size(), "两个不同的方法应该分别被缓存");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("边界场景 - 返回 null")
        @SuppressWarnings("unchecked")
        void testCallWithNullResult() {
            // Arrange
            RpcResponse response = RpcResponse.success("test-request-id", null);
            mockRestTemplateResponse(response);

            // Act
            Object result = httpRpcClient.call(TestService.class, "echo", "Hello");

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("边界场景 - 方法不存在")
        @SuppressWarnings("unchecked")
        void testCallWithNonExistentMethod() {
            // Arrange
            RpcResponse response = RpcResponse.success("test-request-id", "result");
            mockRestTemplateResponse(response);

            // Act
            Object result = httpRpcClient.call(TestService.class, "nonExistentMethod", "arg");

            // Assert
            // 由于方法不存在，无法获取返回类型，但仍然返回原始结果
            assertEquals("result", result);
        }

        @Test
        @DisplayName("边界场景 - 无参数调用")
        @SuppressWarnings("unchecked")
        void testCallWithNoParameters() {
            // Arrange
            RpcResponse response = RpcResponse.success("test-request-id", null);
            mockRestTemplateResponse(response);

            // Act
            httpRpcClient.call(TestService.class, "noReturn");

            // Assert - 不应抛出异常
            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(RpcResponse.class)
            );
        }
    }
}
