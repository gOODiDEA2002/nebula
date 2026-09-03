package io.nebula.examples.rag;

import io.nebula.examples.rag.config.RagDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * RAG 示例应用入口。
 * <p>
 * 演示 {@code nebula-ai-rag} 从「灌库到问答」的完整链路：
 * DocumentSource -> IndexingPipeline（解析、确定性切块、双 sink 写入、状态库）
 * -> 多路检索 -> RRF 融合 -> 防注入清洗 -> 生成 / 流式。
 * <p>
 * {@link RagDemoProperties} 无条件启用，保证禁用态下服务层仍能注入示例配置。
 *
 * @author Nebula Framework
 */
@SpringBootApplication
@EnableConfigurationProperties(RagDemoProperties.class)
public class RagExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagExampleApplication.class, args);
    }
}
