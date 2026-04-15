package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * HttpRpcClient 单元测试
 */
class HttpRpcClientTest {
    
    private HttpRpcClient rpcClient;
    
    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.builder().build();
        Executor executor = Executors.newFixedThreadPool(2);
        ObjectMapper objectMapper = new ObjectMapper();
        rpcClient = new HttpRpcClient(restClient, "http://localhost:8080", executor, objectMapper);
    }
    
    @Test
    void testClientInitialization() {
        assertThat(rpcClient).isNotNull();
    }
    
    @Test
    void testSetTargetAddress() {
        rpcClient.setTargetAddress("192.168.1.100:8080");
        assertThat(rpcClient.getServiceAddress("test")).startsWith("http://192.168.1.100:8080");
    }
}
