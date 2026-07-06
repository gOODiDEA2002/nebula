package io.nebula.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证脱敏 introspector 被挂到 Spring MVC 使用的主 ObjectMapper。
 * <p>
 * 修复前 introspector 只挂在独立的 dataMaskingObjectMapper 上, 控制器正常返回走主 mapper 并不脱敏,
 * {@code @SensitiveData} 事实上不生效。本测试用修复引入的 customizer 构建主 mapper, 断言脱敏生效,
 * 且非敏感字段仍正常序列化(证明 AnnotationIntrospectorPair 未丢默认注解处理)。
 */
class SensitiveDataMaskingCustomizerTest {

    @Test
    void maskingAppliesOnMainObjectMapper() throws Exception {
        DataMaskingStrategyManager manager = new DataMaskingStrategyManager();
        SensitiveDataAnnotationIntrospector introspector = new SensitiveDataAnnotationIntrospector(manager);

        Jackson2ObjectMapperBuilderCustomizer customizer =
                new WebAuthAutoConfiguration().sensitiveDataMaskingCustomizer(introspector);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        ObjectMapper mapper = builder.build();

        String json = mapper.writeValueAsString(new UserView("13812348888", "alice"));

        assertThat(json).contains("138****8888");   // 手机号脱敏生效
        assertThat(json).contains("alice");           // 非敏感字段不变(默认处理保留)
        assertThat(json).doesNotContain("13812348888");
    }

    static class UserView {
        @SensitiveData(type = MaskType.PHONE)
        public String phone;
        public String name;

        UserView(String phone, String name) {
            this.phone = phone;
            this.name = name;
        }
    }
}
