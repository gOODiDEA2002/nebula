package io.nebula.rpc.grpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证 gRPC 服务端 findMethod 仅按"类型名字符串"匹配、不做 Class.forName（杜绝任意类加载）。
 * 修复前 parseParameterTypes 对请求携带的任意类名调 Class.forName，
 * 恶意类名可触发任意类静态初始化（与 HTTP 侧 F-A7 同源，修复策略一致）。
 */
class GrpcRpcServerFindMethodTest {

    private final GrpcRpcServer server = new GrpcRpcServer(new ObjectMapper());

    @Test
    void matchesByDeclaredTypeName() {
        Method m = server.findMethod(Svc.class, "greet", List.of("java.lang.String"));
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("greet");
        assertThat(m.getParameterTypes()[0]).isEqualTo(String.class);
    }

    @Test
    void matchesPrimitiveDeclaredType() {
        // 客户端按 Method.getParameterTypes() 发送时基本类型名为 "int"
        Method m = server.findMethod(Svc.class, "plus", List.of("int", "int"));
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("plus");
    }

    @Test
    void fallsBackByNameAndCountForRuntimeConcreteType() {
        // 客户端发送运行时具体类型 ArrayList，声明为 List：类型名不等，靠名称+参数数量兜底
        Method m = server.findMethod(Svc.class, "process", List.of("java.util.ArrayList"));
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("process");
    }

    @Test
    void fallsBackForWrapperTypeAgainstPrimitiveDeclaration() {
        // GrpcRpcClient 对非 null 参数发送运行时类型（如 java.lang.Integer），声明可能为 int：兜底匹配
        Method m = server.findMethod(Svc.class, "plus", List.of("java.lang.Integer", "java.lang.Integer"));
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("plus");
    }

    @Test
    void bogusTypeNameDoesNotLoadClassAndStillResolves() {
        // 恶意/不存在的类名不应触发类加载(不抛 ClassNotFoundException)，且仍按名称+数量解析
        assertThatCode(() -> {
            Method m = server.findMethod(Svc.class, "greet", List.of("evil.NonExistent$Gadget"));
            assertThat(m).isNotNull();
            assertThat(m.getName()).isEqualTo("greet");
        }).doesNotThrowAnyException();
    }

    @Test
    void unknownMethodReturnsNull() {
        Method m = server.findMethod(Svc.class, "nonExistent", List.of("java.lang.String"));
        assertThat(m).isNull();
    }

    interface Svc {
        String greet(String name);

        int plus(int a, int b);

        int process(List<String> items);
    }
}
