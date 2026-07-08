package io.nebula.rpc.http.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import io.nebula.rpc.http.client.HttpRpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HTTP RPC auth token 全链路测试。
 * <p>
 * 分两层断言(外部审查 P3-2):
 * <ul>
 *   <li>controller 层(不经 client): 直接构造带/不带头的请求调 handleRpcRequest, 断言 401 状态</li>
 *   <li>client 层(走 HttpRpcClient): sendRequest 遇 401 被 catch 封装为 RpcResponse.exception,
 *       call() 走失败分支抛 RuntimeException; 断言异常信息, 不断言原始 HTTP 异常</li>
 * </ul>
 */
@SpringBootTest(
        classes = HttpRpcAuthTokenRoundTripTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nebula.rpc.http.server.auth-token=secret-token"
)
class HttpRpcAuthTokenRoundTripTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpRpcController controller;

    // ========== Controller 层测试 ==========

    @Test
    void controller_noTokenReturns401() {
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(
                echoRequest(), new MockHttpServletRequest());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void controller_correctTokenReturnsSuccess() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader(HttpRpcController.AUTH_TOKEN_HEADER, "secret-token");
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(echoRequest(), http);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isTrue();
        assertThat(resp.getBody().getResult()).isEqualTo("echo:hello");
    }

    // ========== Client 层测试 ==========

    @Test
    void client_withTokenCallsSuccessfully() {
        HttpRpcClient client = createClient("secret-token");
        try {
            EchoService proxy = client.createProxy(EchoService.class);
            String result = proxy.echo("hello");
            assertThat(result).isEqualTo("echo:hello");
        } finally {
            client.close();
        }
    }

    @Test
    void client_withoutTokenGetsFailure() {
        HttpRpcClient client = createClient("");
        try {
            EchoService proxy = client.createProxy(EchoService.class);
            assertThatThrownBy(() -> proxy.echo("hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("RPC调用失败");
        } finally {
            client.close();
        }
    }

    // ========== Helpers ==========

    private HttpRpcClient createClient(String authToken) {
        RestClient restClient = RestClient.create();
        return new HttpRpcClient(restClient, "http://localhost:" + port,
                ForkJoinPool.commonPool(), objectMapper, authToken);
    }

    private RpcRequest echoRequest() {
        RpcRequest r = new RpcRequest();
        r.setRequestId("test-1");
        r.setServiceName(EchoService.class.getName());
        r.setMethodName("echo");
        r.setParameters(new Object[]{"hello"});
        r.setParameterTypes(new String[]{"java.lang.String"});
        return r;
    }

    // ========== Test App ==========

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        HttpRpcServer httpRpcServer() {
            HttpRpcServer server = new HttpRpcServer();
            server.registerService(EchoService.class, new EchoServiceImpl());
            return server;
        }

        @Bean
        HttpRpcController httpRpcController(HttpRpcServer server, ObjectMapper om,
                @Value("${nebula.rpc.http.server.auth-token:}") String authToken) {
            return new HttpRpcController(server, om, authToken);
        }
    }

    interface EchoService {
        String echo(String input);
    }

    static class EchoServiceImpl implements EchoService {
        @Override
        public String echo(String input) {
            return "echo:" + input;
        }
    }
}
