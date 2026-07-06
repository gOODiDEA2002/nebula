package com.acme.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import io.nebula.autoconfigure.data.CacheAutoConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Redis 值序列化 ObjectMapper 的多态白名单：
 * 修复前 allowIfBaseType(Object.class) 放开全部多态反序列化(Jackson gadget RCE)；
 * 修复后仅白名单类型可反序列化。测试特意放在 com.acme 包，使 {@link Untrusted} 属于未信任包。
 */
class CacheRedisTypingWhitelistTest {

    // 用一个"信任一切"的 mapper 生成携带类型信息的 JSON，模拟 Redis 中被写入的多态数据
    private String typedJson(Object value) throws Exception {
        ObjectMapper permissive = new ObjectMapper();
        permissive.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        return permissive.writeValueAsString(value);
    }

    @Test
    void trustedJdkContainerTypeRoundTrips() throws Exception {
        ObjectMapper mapper = CacheAutoConfiguration.buildRedisValueObjectMapper(List.of());
        Object value = new ArrayList<>(List.of("a", "b"));
        Object back = mapper.readValue(mapper.writeValueAsString(value), Object.class);
        assertThat(back).isEqualTo(value);
    }

    @Test
    void untrustedTypeIsRejectedByDefault() throws Exception {
        String malicious = typedJson(new Untrusted("x"));
        ObjectMapper restricted = CacheAutoConfiguration.buildRedisValueObjectMapper(List.of());
        // com.acme 不在默认白名单(java.util/java.time/io.nebula) -> 反序列化被拒
        assertThatThrownBy(() -> restricted.readValue(malicious, Object.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void configuredTrustedPackageIsAllowed() throws Exception {
        String json = typedJson(new Untrusted("x"));
        ObjectMapper trusting = CacheAutoConfiguration.buildRedisValueObjectMapper(List.of("com.acme."));
        Object back = trusting.readValue(json, Object.class);
        assertThat(back).isInstanceOf(Untrusted.class);
        assertThat(((Untrusted) back).getName()).isEqualTo("x");
    }

    /** 非 final 的简单 POJO，位于未信任包 com.acme。 */
    public static class Untrusted {
        private String name;

        public Untrusted() {
        }

        public Untrusted(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
