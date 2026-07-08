package io.nebula.starter.all;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 nebula-starter-all 的 nebula-defaults.properties 注入了
 * preferred-json-mapper=jackson2, 保证 MVC 消息转换器走 Jackson 2 路径。
 */
@SpringBootTest(classes = StarterDefaultsInjectionTest.MinimalApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StarterDefaultsInjectionTest {

    @Autowired
    private Environment environment;

    @Test
    void preferredJsonMapperDefaultsToJackson2() {
        assertThat(environment.getProperty("spring.http.converters.preferred-json-mapper"))
                .isEqualTo("jackson2");
    }

    @SpringBootConfiguration
    static class MinimalApp {
    }
}
