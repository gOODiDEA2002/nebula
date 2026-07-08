package io.nebula.rpc.http.server;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * 验证 findMethod 仅按"类型名字符串"匹配、不做 Class.forName（杜绝任意类加载）。
 * 修复前 parameterTypes 为 Class&lt;?&gt;[]，Jackson 反序列化会对请求携带的任意类名调 Class.forName。
 */
class HttpRpcControllerFindMethodTest {

    private final HttpRpcController controller =
            new HttpRpcController(mock(HttpRpcServer.class), JsonMapper.builder().build());

    @Test
    void matchesByDeclaredTypeName() {
        Method m = controller.findMethod(Svc.class, "greet", new String[]{"java.lang.String"});
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("greet");
    }

    @Test
    void fallsBackByNameAndCountForRuntimeConcreteType() {
        // 客户端发送运行时具体类型 ArrayList，声明为 List：类型名不等，靠名称+参数数量兜底
        Method m = controller.findMethod(Svc.class, "process", new String[]{"java.util.ArrayList"});
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("process");
    }

    @Test
    void bogusTypeNameDoesNotLoadClassAndStillResolves() {
        // 恶意/不存在的类名不应触发类加载(不抛 ClassNotFoundException)，且仍按名称+数量解析
        assertThatCode(() -> {
            Method m = controller.findMethod(Svc.class, "greet", new String[]{"evil.NonExistent$Gadget"});
            assertThat(m).isNotNull();
            assertThat(m.getName()).isEqualTo("greet");
        }).doesNotThrowAnyException();
    }

    interface Svc {
        String greet(String name);

        int process(List<String> items);
    }
}
