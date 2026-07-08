package io.nebula.rpc.http.server;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证 /rpc 端点的可选 token 鉴权(默认关闭)。
 */
class HttpRpcControllerAuthTest {

    private final HttpRpcServer server = mock(HttpRpcServer.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private RpcRequest request() {
        RpcRequest r = new RpcRequest();
        r.setRequestId("1");
        r.setServiceName("SomeService");
        r.setMethodName("m");
        return r;
    }

    @Test
    void rejectsWhenTokenConfiguredButHeaderMissing() {
        HttpRpcController controller = new HttpRpcController(server, objectMapper, "secret");
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(request(), new MockHttpServletRequest());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(server); // 未通过鉴权, 不触碰服务注册表
    }

    @Test
    void rejectsWhenTokenWrong() {
        HttpRpcController controller = new HttpRpcController(server, objectMapper, "secret");
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader(HttpRpcController.AUTH_TOKEN_HEADER, "wrong");
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(request(), http);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void passesWhenTokenMatches() {
        when(server.getServiceRegistry()).thenReturn(new ConcurrentHashMap<>());
        HttpRpcController controller = new HttpRpcController(server, objectMapper, "secret");
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader(HttpRpcController.AUTH_TOKEN_HEADER, "secret");
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(request(), http);
        // 通过鉴权后走到服务查找, 服务未注册返回 404(证明鉴权已放行)
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void noAuthWhenTokenEmpty() {
        when(server.getServiceRegistry()).thenReturn(new ConcurrentHashMap<>());
        HttpRpcController controller = new HttpRpcController(server, objectMapper); // 无 token
        ResponseEntity<RpcResponse> resp = controller.handleRpcRequest(request(), new MockHttpServletRequest());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
