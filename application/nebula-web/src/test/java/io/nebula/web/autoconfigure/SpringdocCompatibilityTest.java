package io.nebula.web.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 Springdoc 3.x 能在 Spring Boot 4.1 Web 上下文中生成 OpenAPI 文档。
 */
@SpringBootTest(classes = SpringdocCompatibilityTest.TestApp.class)
@AutoConfigureMockMvc
class SpringdocCompatibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.version").value("2.1.0"))
                .andExpect(jsonPath("$.paths['/springdoc-smoke']").exists());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(SmokeController.class)
    static class TestApp {
    }

    @RestController
    static class SmokeController {
        @GetMapping("/springdoc-smoke")
        String smoke() {
            return "ok";
        }
    }
}
