package io.nebula.rpc.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import io.nebula.rpc.grpc.test.ComplexUser;
import io.nebula.rpc.grpc.test.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RPC消息转换器测试类
 * 测试Java对象与JSON字符串之间的序列化和反序列化
 */
class RpcMessageConverterTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试对象转JSON
     */
    @Test
    void testObjectToJson() throws Exception {
        // 准备测试对象
        TestUser user = new TestUser(123L, "TestUser", 30);
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(user);
        
        // 验证JSON包含预期字段
        assertThat(json).contains("\"id\":123");
        assertThat(json).contains("\"name\":\"TestUser\"");
        assertThat(json).contains("\"age\":30");
    }

    /**
     * 测试JSON转对象
     */
    @Test
    void testJsonToObject() throws Exception {
        // 准备JSON字符串
        String json = "{\"id\":456,\"name\":\"AnotherUser\",\"age\":25}";
        
        // 反序列化为对象
        TestUser user = objectMapper.readValue(json, TestUser.class);
        
        // 验证对象属性
        assertThat(user.getId()).isEqualTo(456L);
        assertThat(user.getName()).isEqualTo("AnotherUser");
        assertThat(user.getAge()).isEqualTo(25);
    }

    /**
     * 测试序列化基本类型
     */
    @Test
    void testSerializePrimitiveTypes() throws Exception {
        // 测试字符串
        String stringJson = objectMapper.writeValueAsString("Hello");
        assertThat(stringJson).isEqualTo("\"Hello\"");
        
        // 测试整数
        String intJson = objectMapper.writeValueAsString(123);
        assertThat(intJson).isEqualTo("123");
        
        // 测试布尔值
        String boolJson = objectMapper.writeValueAsString(true);
        assertThat(boolJson).isEqualTo("true");
    }

    /**
     * 测试反序列化基本类型
     */
    @Test
    void testDeserializePrimitiveTypes() throws Exception {
        // 测试字符串
        String str = objectMapper.readValue("\"Hello\"", String.class);
        assertThat(str).isEqualTo("Hello");
        
        // 测试整数
        Integer num = objectMapper.readValue("123", Integer.class);
        assertThat(num).isEqualTo(123);
        
        // 测试布尔值
        Boolean bool = objectMapper.readValue("true", Boolean.class);
        assertThat(bool).isTrue();
    }

    /**
     * 测试构建RPC请求消息
     */
    @Test
    void testBuildRpcRequest() throws Exception {
        // 构建请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("req-001")
                .setServiceName("com.example.TestService")
                .setMethodName("testMethod")
                .addParameterTypes(String.class.getName())
                .addParameterTypes(Integer.class.getName())
                .addParameters(objectMapper.writeValueAsString("param1"))
                .addParameters(objectMapper.writeValueAsString(100))
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 验证请求属性
        assertThat(request.getRequestId()).isEqualTo("req-001");
        assertThat(request.getServiceName()).isEqualTo("com.example.TestService");
        assertThat(request.getMethodName()).isEqualTo("testMethod");
        assertThat(request.getParameterTypesCount()).isEqualTo(2);
        assertThat(request.getParametersCount()).isEqualTo(2);
        assertThat(request.getTimestamp()).isGreaterThan(0);
    }

    /**
     * 测试构建RPC响应消息（成功）
     */
    @Test
    void testBuildRpcResponseSuccess() throws Exception {
        // 准备返回值
        TestUser user = new TestUser(789L, "ResponseUser", 35);
        String resultJson = objectMapper.writeValueAsString(user);
        
        // 构建响应
        RpcResponse response = RpcResponse.newBuilder()
                .setRequestId("req-002")
                .setSuccess(true)
                .setResult(resultJson)
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 验证响应属性
        assertThat(response.getRequestId()).isEqualTo("req-002");
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getResult()).contains("ResponseUser");
        assertThat(response.getTimestamp()).isGreaterThan(0);
    }

    /**
     * 测试构建RPC响应消息（失败）
     */
    @Test
    void testBuildRpcResponseFailure() {
        // 构建失败响应
        RpcResponse response = RpcResponse.newBuilder()
                .setRequestId("req-003")
                .setSuccess(false)
                .setErrorCode("ERR_001")
                .setErrorMessage("测试错误消息")
                .setStackTrace("at com.example.Test.method(Test.java:10)")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 验证响应属性
        assertThat(response.getRequestId()).isEqualTo("req-003");
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("ERR_001");
        assertThat(response.getErrorMessage()).isEqualTo("测试错误消息");
        assertThat(response.getStackTrace()).contains("Test.java");
    }

    /**
     * 测试序列化参数列表
     */
    @Test
    void testSerializeParameters() throws Exception {
        // 准备多个参数
        Object[] parameters = new Object[]{
            "stringParam",
            123,
            true,
            new TestUser(111L, "ParamUser", 20)
        };
        
        // 序列化所有参数
        String[] serializedParams = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            serializedParams[i] = objectMapper.writeValueAsString(parameters[i]);
        }
        
        // 验证序列化结果
        assertThat(serializedParams[0]).isEqualTo("\"stringParam\"");
        assertThat(serializedParams[1]).isEqualTo("123");
        assertThat(serializedParams[2]).isEqualTo("true");
        assertThat(serializedParams[3]).contains("ParamUser");
    }

    /**
     * 测试反序列化参数列表
     */
    @Test
    void testDeserializeParameters() throws Exception {
        // 准备序列化的参数
        String[] serializedParams = new String[]{
            "\"stringParam\"",
            "123",
            "true",
            "{\"id\":222,\"name\":\"DeserializedUser\",\"age\":28}"
        };
        
        // 准备参数类型
        Class<?>[] parameterTypes = new Class<?>[]{
            String.class,
            Integer.class,
            Boolean.class,
            TestUser.class
        };
        
        // 反序列化所有参数
        Object[] parameters = new Object[serializedParams.length];
        for (int i = 0; i < serializedParams.length; i++) {
            parameters[i] = objectMapper.readValue(serializedParams[i], parameterTypes[i]);
        }
        
        // 验证反序列化结果
        assertThat(parameters[0]).isEqualTo("stringParam");
        assertThat(parameters[1]).isEqualTo(123);
        assertThat(parameters[2]).isEqualTo(true);
        assertThat(((TestUser) parameters[3]).getName()).isEqualTo("DeserializedUser");
    }

    /**
     * 测试反序列化返回值
     */
    @Test
    void testDeserializeResult() throws Exception {
        // 准备序列化的返回值
        String resultJson = "{\"id\":333,\"name\":\"ResultUser\",\"age\":32}";
        
        // 反序列化返回值
        TestUser result = objectMapper.readValue(resultJson, TestUser.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(333L);
        assertThat(result.getName()).isEqualTo("ResultUser");
        assertThat(result.getAge()).isEqualTo(32);
    }

    /**
     * 测试处理null值
     */
    @Test
    void testHandleNullValues() throws Exception {
        // 序列化null
        String nullJson = objectMapper.writeValueAsString(null);
        assertThat(nullJson).isEqualTo("null");
        
        // 反序列化null (字符串"null")
        TestUser nullUser = objectMapper.readValue("null", TestUser.class);
        assertThat(nullUser).isNull();
    }

    /**
     * 测试RPC请求包含元数据
     */
    @Test
    void testRpcRequestWithMetadata() {
        // 构建带元数据的请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("req-004")
                .setServiceName("com.example.MetadataService")
                .setMethodName("testMethod")
                .putMetadata("traceId", "trace-123")
                .putMetadata("userId", "user-456")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 验证元数据
        assertThat(request.getMetadataMap()).containsEntry("traceId", "trace-123");
        assertThat(request.getMetadataMap()).containsEntry("userId", "user-456");
    }

    // ========== 复杂对象转换测试 ==========

    /**
     * 测试复杂对象序列化（包含嵌套对象）
     * 场景：序列化包含 Address、List<Order> 的 ComplexUser 对象
     * 验证：JSON 包含所有嵌套字段
     */
    @Test
    void testSerializeComplexObject() throws Exception {
        // 准备复杂对象
        ComplexUser.Address address = new ComplexUser.Address("123 Main St", "New York", "10001");
        
        List<ComplexUser.Order> orders = Arrays.asList(
            new ComplexUser.Order("ORD-001", 99.99, "COMPLETED"),
            new ComplexUser.Order("ORD-002", 149.50, "PENDING")
        );
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("vipLevel", "Gold");
        metadata.put("registeredYear", "2020");
        
        ComplexUser complexUser = new ComplexUser(
            1001L,
            "ComplexUser",
            address,
            orders,
            metadata
        );
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(complexUser);
        
        // 验证JSON包含所有字段
        assertThat(json).contains("\"id\":1001");
        assertThat(json).contains("\"name\":\"ComplexUser\"");
        
        // 验证嵌套对象 Address
        assertThat(json).contains("\"street\":\"123 Main St\"");
        assertThat(json).contains("\"city\":\"New York\"");
        assertThat(json).contains("\"zipCode\":\"10001\"");
        
        // 验证嵌套列表 Orders
        assertThat(json).contains("\"orderId\":\"ORD-001\"");
        assertThat(json).contains("\"amount\":99.99");
        assertThat(json).contains("\"status\":\"COMPLETED\"");
        assertThat(json).contains("\"orderId\":\"ORD-002\"");
        
        // 验证 Map
        assertThat(json).contains("\"vipLevel\":\"Gold\"");
        assertThat(json).contains("\"registeredYear\":\"2020\"");
    }

    /**
     * 测试复杂对象反序列化（包含嵌套对象）
     * 场景：从 JSON 反序列化为 ComplexUser 对象
     * 验证：所有嵌套字段正确反序列化
     */
    @Test
    void testDeserializeComplexObject() throws Exception {
        // 准备包含嵌套对象的JSON
        String json = "{"
            + "\"id\":2001,"
            + "\"name\":\"DeserializedComplexUser\","
            + "\"address\":{"
            +   "\"street\":\"456 Oak Ave\","
            +   "\"city\":\"Los Angeles\","
            +   "\"zipCode\":\"90001\""
            + "},"
            + "\"orders\":["
            +   "{\"orderId\":\"ORD-101\",\"amount\":79.99,\"status\":\"SHIPPED\"},"
            +   "{\"orderId\":\"ORD-102\",\"amount\":199.99,\"status\":\"DELIVERED\"}"
            + "],"
            + "\"metadata\":{"
            +   "\"memberSince\":\"2019\","
            +   "\"preferredLanguage\":\"EN\""
            + "}"
            + "}";
        
        // 反序列化为对象
        ComplexUser complexUser = objectMapper.readValue(json, ComplexUser.class);
        
        // 验证主对象属性
        assertThat(complexUser).isNotNull();
        assertThat(complexUser.getId()).isEqualTo(2001L);
        assertThat(complexUser.getName()).isEqualTo("DeserializedComplexUser");
        
        // 验证嵌套对象 Address
        assertThat(complexUser.getAddress()).isNotNull();
        assertThat(complexUser.getAddress().getStreet()).isEqualTo("456 Oak Ave");
        assertThat(complexUser.getAddress().getCity()).isEqualTo("Los Angeles");
        assertThat(complexUser.getAddress().getZipCode()).isEqualTo("90001");
        
        // 验证嵌套列表 Orders
        assertThat(complexUser.getOrders()).isNotNull();
        assertThat(complexUser.getOrders()).hasSize(2);
        assertThat(complexUser.getOrders().get(0).getOrderId()).isEqualTo("ORD-101");
        assertThat(complexUser.getOrders().get(0).getAmount()).isEqualTo(79.99);
        assertThat(complexUser.getOrders().get(0).getStatus()).isEqualTo("SHIPPED");
        assertThat(complexUser.getOrders().get(1).getOrderId()).isEqualTo("ORD-102");
        
        // 验证 Map
        assertThat(complexUser.getMetadata()).isNotNull();
        assertThat(complexUser.getMetadata()).containsEntry("memberSince", "2019");
        assertThat(complexUser.getMetadata()).containsEntry("preferredLanguage", "EN");
    }

    /**
     * 测试序列化包含null字段的复杂对象
     * 场景：ComplexUser 的某些嵌套字段为 null
     * 验证：能正确处理 null 字段
     */
    @Test
    void testSerializeComplexObjectWithNullFields() throws Exception {
        // 准备包含 null 字段的复杂对象
        ComplexUser complexUser = new ComplexUser(
            3001L,
            "UserWithNulls",
            null,  // address 为 null
            null,  // orders 为 null
            null   // metadata 为 null
        );
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(complexUser);
        
        // 验证JSON包含主字段
        assertThat(json).contains("\"id\":3001");
        assertThat(json).contains("\"name\":\"UserWithNulls\"");
        
        // 验证null字段被包含
        assertThat(json).contains("\"address\":null");
        assertThat(json).contains("\"orders\":null");
        assertThat(json).contains("\"metadata\":null");
    }

    /**
     * 测试反序列化包含null字段的复杂对象
     * 场景：JSON 中某些嵌套字段为 null
     * 验证：能正确处理 null 字段
     */
    @Test
    void testDeserializeComplexObjectWithNullFields() throws Exception {
        // 准备包含 null 字段的JSON
        String json = "{"
            + "\"id\":4001,"
            + "\"name\":\"UserWithNulls\","
            + "\"address\":null,"
            + "\"orders\":null,"
            + "\"metadata\":null"
            + "}";
        
        // 反序列化为对象
        ComplexUser complexUser = objectMapper.readValue(json, ComplexUser.class);
        
        // 验证主对象属性
        assertThat(complexUser).isNotNull();
        assertThat(complexUser.getId()).isEqualTo(4001L);
        assertThat(complexUser.getName()).isEqualTo("UserWithNulls");
        
        // 验证null字段
        assertThat(complexUser.getAddress()).isNull();
        assertThat(complexUser.getOrders()).isNull();
        assertThat(complexUser.getMetadata()).isNull();
    }

    /**
     * 测试往返转换（序列化 + 反序列化）
     * 场景：复杂对象先序列化为JSON，再反序列化回对象
     * 验证：往返后对象保持一致
     */
    @Test
    void testComplexObjectRoundTrip() throws Exception {
        // 准备原始复杂对象
        ComplexUser.Address originalAddress = new ComplexUser.Address("789 Pine Rd", "Chicago", "60601");
        
        List<ComplexUser.Order> originalOrders = Arrays.asList(
            new ComplexUser.Order("ORD-201", 299.99, "PROCESSING")
        );
        
        Map<String, String> originalMetadata = new HashMap<>();
        originalMetadata.put("accountType", "Premium");
        
        ComplexUser originalUser = new ComplexUser(
            5001L,
            "RoundTripUser",
            originalAddress,
            originalOrders,
            originalMetadata
        );
        
        // 序列化
        String json = objectMapper.writeValueAsString(originalUser);
        
        // 反序列化
        ComplexUser deserializedUser = objectMapper.readValue(json, ComplexUser.class);
        
        // 验证往返后的对象与原始对象一致
        assertThat(deserializedUser.getId()).isEqualTo(originalUser.getId());
        assertThat(deserializedUser.getName()).isEqualTo(originalUser.getName());
        assertThat(deserializedUser.getAddress().getStreet()).isEqualTo(originalAddress.getStreet());
        assertThat(deserializedUser.getAddress().getCity()).isEqualTo(originalAddress.getCity());
        assertThat(deserializedUser.getAddress().getZipCode()).isEqualTo(originalAddress.getZipCode());
        assertThat(deserializedUser.getOrders()).hasSize(1);
        assertThat(deserializedUser.getOrders().get(0).getOrderId()).isEqualTo("ORD-201");
        assertThat(deserializedUser.getOrders().get(0).getAmount()).isEqualTo(299.99);
        assertThat(deserializedUser.getMetadata()).containsEntry("accountType", "Premium");
    }
}

