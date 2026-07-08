package io.nebula.web.autoconfigure;

import io.nebula.core.common.result.Result;
import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc 脱敏回归哨兵测试。
 * <p>
 * 在 MVC 转换器路径（而非 mapper 本体）断言脱敏生效, 防止再次静默回归。
 * Jackson 3 迁移后不再需要 preferred-json-mapper=jackson2 属性——
 * 脱敏通过 Jackson 3 原生 JsonMapperBuilderCustomizer 路径生效。
 */
@SpringBootTest(
        classes = SensitiveDataMvcMaskingTest.TestApp.class,
        properties = {
                "nebula.web.data-masking.enabled=true"
        }
)
@AutoConfigureMockMvc
class SensitiveDataMvcMaskingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sensitiveFieldMaskedInMvcResponse() throws Exception {
        mockMvc.perform(get("/test/masking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("138****5678"))
                .andExpect(jsonPath("$.data.name").value("alice"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(SensitiveDataMvcMaskingTest.MaskingTestController.class)
    static class TestApp {
    }

    @RestController
    static class MaskingTestController {

        @GetMapping("/test/masking")
        public Result<UserDto> getMaskedUser() {
            return Result.success(new UserDto("13812345678", "alice"));
        }
    }

    static class UserDto {
        @SensitiveData(type = MaskType.PHONE)
        private String phone;
        private String name;

        UserDto(String phone, String name) {
            this.phone = phone;
            this.name = name;
        }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
