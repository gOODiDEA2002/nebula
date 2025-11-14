package io.nebula.core.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * JsonUtils单元测试
 */
class JsonUtilsTest {
    
    // 测试POJO类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private String id;
        private String name;
        private int age;
        private List<String> roles;
    }
    
    // ====================
    // 序列化测试
    // ====================
    
    @Test
    void testToJson() {
        TestUser user = new TestUser("1", "张三", 25, Arrays.asList("USER", "ADMIN"));
        
        String json = JsonUtils.toJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("\"id\":\"1\"");
        assertThat(json).contains("\"name\":\"张三\"");
        assertThat(json).contains("\"age\":25");
    }
    
    @Test
    void testToJsonNull() {
        String json = JsonUtils.toJson(null);
        
        assertThat(json).isNull();
    }
    
    @Test
    void testToPrettyJson() {
        TestUser user = new TestUser("1", "张三", 25, Arrays.asList("USER"));
        
        String json = JsonUtils.toPrettyJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("\n");  // 美化输出应包含换行
        assertThat(json).contains("  ");  // 美化输出应包含缩进
    }
    
    @Test
    void testToJsonBytes() {
        TestUser user = new TestUser("1", "张三", 25, null);
        
        byte[] bytes = JsonUtils.toJsonBytes(user);
        
        assertThat(bytes).isNotNull();
        assertThat(bytes).isNotEmpty();
    }
    
    // ====================
    // 反序列化测试
    // ====================
    
    @Test
    void testFromJson() {
        String json = "{\"id\":\"1\",\"name\":\"张三\",\"age\":25,\"roles\":[\"USER\"]}";
        
        TestUser user = JsonUtils.fromJson(json, TestUser.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("1");
        assertThat(user.getName()).isEqualTo("张三");
        assertThat(user.getAge()).isEqualTo(25);
        assertThat(user.getRoles()).containsExactly("USER");
    }
    
    @Test
    void testFromJsonNull() {
        TestUser user = JsonUtils.fromJson((String) null, TestUser.class);
        
        assertThat(user).isNull();
    }
    
    @Test
    void testFromJsonInvalidJson() {
        TestUser user = JsonUtils.fromJson("invalid json", TestUser.class);
        
        assertThat(user).isNull();
    }
    
    @Test
    void testFromJsonWithTypeReference() {
        String json = "[{\"id\":\"1\",\"name\":\"张三\",\"age\":25},{\"id\":\"2\",\"name\":\"李四\",\"age\":30}]";
        
        TypeReference<List<TestUser>> typeRef = new TypeReference<List<TestUser>>() {};
        List<TestUser> users = JsonUtils.fromJson(json, typeRef);
        
        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("张三");
        assertThat(users.get(1).getName()).isEqualTo("李四");
    }
    
    @Test
    void testFromJsonBytes() {
        String json = "{\"id\":\"1\",\"name\":\"张三\",\"age\":25}";
        byte[] bytes = json.getBytes();
        
        TestUser user = JsonUtils.fromJsonBytes(bytes, TestUser.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("张三");
    }
    
    // ====================
    // 特殊类型转换测试
    // ====================
    
    @Test
    void testToMap() {
        String json = "{\"id\":\"1\",\"name\":\"张三\",\"age\":25}";
        
        Map<String, Object> map = JsonUtils.toMap(json);
        
        assertThat(map).isNotNull();
        assertThat(map.get("id")).isEqualTo("1");
        assertThat(map.get("name")).isEqualTo("张三");
        assertThat(map.get("age")).isEqualTo(25);
    }
    
    @Test
    void testToList() {
        String json = "[{\"id\":\"1\",\"name\":\"张三\"},{\"id\":\"2\",\"name\":\"李四\"}]";
        
        List<TestUser> users = JsonUtils.toList(json, TestUser.class);
        
        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isEqualTo("1");
        assertThat(users.get(1).getId()).isEqualTo("2");
    }
    
    @Test
    void testObjectToMap() {
        TestUser user = new TestUser("1", "张三", 25, Arrays.asList("USER"));
        
        Map<String, Object> map = JsonUtils.objectToMap(user);
        
        assertThat(map).isNotNull();
        assertThat(map.get("id")).isEqualTo("1");
        assertThat(map.get("name")).isEqualTo("张三");
        assertThat(map.get("age")).isEqualTo(25);
    }
    
    @Test
    void testMapToObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "1");
        map.put("name", "张三");
        map.put("age", 25);
        
        TestUser user = JsonUtils.mapToObject(map, TestUser.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("1");
        assertThat(user.getName()).isEqualTo("张三");
        assertThat(user.getAge()).isEqualTo(25);
    }
    
    // ====================
    // JSON节点操作测试
    // ====================
    
    @Test
    void testParseJson() {
        String json = "{\"user\":{\"name\":\"张三\",\"age\":25}}";
        
        JsonNode node = JsonUtils.parseJson(json);
        
        assertThat(node).isNotNull();
        assertThat(node.isObject()).isTrue();
    }
    
    @Test
    void testGetValue() {
        String json = "{\"user\":{\"name\":\"张三\",\"age\":25}}";
        JsonNode node = JsonUtils.parseJson(json);
        
        String name = JsonUtils.getValue(node, "user.name");
        
        assertThat(name).isEqualTo("张三");
    }
    
    @Test
    void testGetValueNotExist() {
        String json = "{\"user\":{\"name\":\"张三\"}}";
        JsonNode node = JsonUtils.parseJson(json);
        
        String value = JsonUtils.getValue(node, "user.notexist");
        
        assertThat(value).isNull();
    }
    
    @Test
    void testGetValueWithType() {
        String json = "{\"user\":{\"age\":25}}";
        JsonNode node = JsonUtils.parseJson(json);
        
        Integer age = JsonUtils.getValue(node, "user.age", Integer.class);
        
        assertThat(age).isEqualTo(25);
    }
    
    // ====================
    // 验证和判断测试
    // ====================
    
    @Test
    void testIsValidJson() {
        String validJson = "{\"name\":\"test\"}";
        
        boolean isValid = JsonUtils.isValidJson(validJson);
        
        assertThat(isValid).isTrue();
    }
    
    @Test
    void testIsValidJsonInvalid() {
        String invalidJson = "{name:test}";  // 缺少引号
        
        boolean isValid = JsonUtils.isValidJson(invalidJson);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testIsValidJsonNull() {
        boolean isValid = JsonUtils.isValidJson(null);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testIsJsonObject() {
        String json = "{\"name\":\"test\"}";
        
        boolean isObject = JsonUtils.isJsonObject(json);
        
        assertThat(isObject).isTrue();
    }
    
    @Test
    void testIsJsonObjectArray() {
        String json = "[{\"name\":\"test\"}]";
        
        boolean isObject = JsonUtils.isJsonObject(json);
        
        assertThat(isObject).isFalse();
    }
    
    @Test
    void testIsJsonArray() {
        String json = "[{\"name\":\"test\"}]";
        
        boolean isArray = JsonUtils.isJsonArray(json);
        
        assertThat(isArray).isTrue();
    }
    
    @Test
    void testIsJsonArrayObject() {
        String json = "{\"name\":\"test\"}";
        
        boolean isArray = JsonUtils.isJsonArray(json);
        
        assertThat(isArray).isFalse();
    }
    
    // ====================
    // JSON合并和操作测试
    // ====================
    
    @Test
    void testMergeJson() {
        String json1 = "{\"name\":\"张三\",\"age\":25}";
        String json2 = "{\"age\":30,\"email\":\"test@example.com\"}";
        
        String merged = JsonUtils.mergeJson(json1, json2);
        
        assertThat(merged).isNotNull();
        Map<String, Object> map = JsonUtils.toMap(merged);
        assertThat(map.get("name")).isEqualTo("张三");
        assertThat(map.get("age")).isEqualTo(30);  // json2覆盖json1
        assertThat(map.get("email")).isEqualTo("test@example.com");
    }
    
    @Test
    void testMergeJsonBothNull() {
        String merged = JsonUtils.mergeJson(null, null);
        
        assertThat(merged).isNull();
    }
    
    @Test
    void testMergeJsonOneNull() {
        String json = "{\"name\":\"test\"}";
        
        String merged1 = JsonUtils.mergeJson(json, null);
        String merged2 = JsonUtils.mergeJson(null, json);
        
        assertThat(merged1).isEqualTo(json);
        assertThat(merged2).isEqualTo(json);
    }
    
    @Test
    void testDeepCopy() {
        TestUser original = new TestUser("1", "张三", 25, new ArrayList<>(Arrays.asList("USER")));
        
        TestUser copy = JsonUtils.deepCopy(original, TestUser.class);
        
        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getName()).isEqualTo(original.getName());
        
        // 修改副本不应影响原对象
        copy.setName("李四");
        assertThat(original.getName()).isEqualTo("张三");
    }
    
    @Test
    void testFormatJson() {
        String compactJson = "{\"name\":\"test\",\"age\":25}";
        
        String formatted = JsonUtils.formatJson(compactJson);
        
        assertThat(formatted).isNotNull();
        assertThat(formatted).contains("\n");
        assertThat(formatted).contains("  ");
    }
    
    @Test
    void testFormatJsonInvalid() {
        String invalidJson = "invalid json";
        
        String formatted = JsonUtils.formatJson(invalidJson);
        
        assertThat(formatted).isEqualTo(invalidJson);  // 无效JSON返回原字符串
    }
    
    // ====================
    // 特殊字符处理测试
    // ====================
    
    @Test
    void testSerializeDeserializeSpecialCharacters() {
        TestUser user = new TestUser("1", "特殊字符: \"引号\" \\反斜杠\\ \n换行", 25, null);
        
        String json = JsonUtils.toJson(user);
        TestUser deserialized = JsonUtils.fromJson(json, TestUser.class);
        
        assertThat(deserialized.getName()).isEqualTo(user.getName());
    }
    
    @Test
    void testSerializeDeserializeEmoji() {
        TestUser user = new TestUser("1", "测试表情", 25, null);
        
        String json = JsonUtils.toJson(user);
        TestUser deserialized = JsonUtils.fromJson(json, TestUser.class);
        
        assertThat(deserialized.getName()).isEqualTo(user.getName());
    }
    
    // ====================
    // 空值处理测试
    // ====================
    
    @Test
    void testSerializeWithNullFields() {
        TestUser user = new TestUser("1", null, 25, null);
        
        String json = JsonUtils.toJson(user);
        
        assertThat(json).isNotNull();
        // Jackson默认会包含null值
    }
    
    @Test
    void testDeserializeEmptyString() {
        TestUser user = JsonUtils.fromJson("", TestUser.class);
        
        assertThat(user).isNull();
    }
    
    @Test
    void testDeserializeBlankString() {
        TestUser user = JsonUtils.fromJson("   ", TestUser.class);
        
        assertThat(user).isNull();
    }
}

