package io.nebula.ai.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Document Loader 简化测试
 * 注意：文档加载测试需要Spring AI的DocumentReader等组件
 */
@ExtendWith(MockitoExtension.class)
class DocumentLoaderTest {

    @Test
    void testPlaceholder() {
        // 文档加载测试需要 Spring AI Document API
        // 这里提供一个占位测试
        assertThat(true).isTrue();
    }
}

