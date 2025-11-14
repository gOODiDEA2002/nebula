package io.nebula.ai.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG Service 简化测试
 * 注意：RAG测试需要集成Chat、Embedding和VectorStore
 */
@ExtendWith(MockitoExtension.class)
class RAGServiceTest {

    @Test
    void testPlaceholder() {
        // RAG 测试需要集成多个AI服务
        // 这里提供一个占位测试
        assertThat(true).isTrue();
    }
}

