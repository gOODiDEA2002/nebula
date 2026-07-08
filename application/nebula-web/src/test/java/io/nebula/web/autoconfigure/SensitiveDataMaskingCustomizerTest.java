package io.nebula.web.autoconfigure;

import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证脱敏 introspector 被挂到 Spring MVC 使用的主 JsonMapper（Jackson 3 路径）。
 */
class SensitiveDataMaskingCustomizerTest {

    @Test
    void maskingAppliesOnMainObjectMapper() throws Exception {
        DataMaskingStrategyManager manager = new DataMaskingStrategyManager();
        SensitiveDataAnnotationIntrospector introspector = new SensitiveDataAnnotationIntrospector(manager);

        JsonMapperBuilderCustomizer customizer =
                new WebAuthAutoConfiguration().sensitiveDataMaskingCustomizer(introspector);

        JsonMapper.Builder builder = JsonMapper.builder();
        customizer.customize(builder);
        JsonMapper mapper = builder.build();

        String json = mapper.writeValueAsString(new UserView("13812348888", "alice"));

        assertThat(json).contains("138****8888");
        assertThat(json).contains("alice");
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
