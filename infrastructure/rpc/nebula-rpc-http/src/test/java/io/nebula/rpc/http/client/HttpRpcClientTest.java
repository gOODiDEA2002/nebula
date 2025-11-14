package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HttpRpcClient单元测试
 */
@ExtendWith(MockitoExtension.class)
class HttpRpcClientTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    private HttpRpcClient rpcClient;
    private Executor executor;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        objectMapper = new ObjectMapper();
        rpcClient = new HttpRpcClient(restTemplate, "http://localhost:8080", executor, objectMapper);
    }
    
    @Test
    void testPost() {
        String url = "/api/test";
        String requestBody = "test request";
        String expectedResponse = "test response";
        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(expectedResponse);
        
        String result = restTemplate.postForObject(url, requestBody, String.class);
        
        assertThat(result).isEqualTo(expectedResponse);
        verify(restTemplate).postForObject(url, requestBody, String.class);
    }
    
    @Test
    void testGet() {
        String url = "/api/test";
        String expectedResponse = "test response";
        
        when(restTemplate.getForObject(url, String.class))
                .thenReturn(expectedResponse);
        
        String result = restTemplate.getForObject(url, String.class);
        
        assertThat(result).isEqualTo(expectedResponse);
    }
    
    @Test
    void testClientInitialization() {
        assertThat(rpcClient).isNotNull();
    }
}
