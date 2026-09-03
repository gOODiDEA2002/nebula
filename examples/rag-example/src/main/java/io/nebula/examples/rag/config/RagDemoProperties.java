package io.nebula.examples.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 示例自有配置，前缀 {@code rag-demo}。
 * <p>
 * 只承载「示例层」需要的三个旋钮：两路检索权重与状态文件路径。
 * 框架侧参数（切块、融合、检索 top-k 等）仍走 {@code nebula.ai.rag.*}，
 * 避免与框架配置重复定义造成漂移。
 *
 * @author Nebula Framework
 */
@ConfigurationProperties(prefix = "rag-demo")
public class RagDemoProperties {

    /** 向量检索器权重，接 {@code VECTOR_WEIGHT} */
    private double vectorWeight = 0.6;

    /** 关键词检索器权重，接 {@code KEYWORD_WEIGHT} */
    private double keywordWeight = 0.4;

    /** 索引状态持久化文件路径 */
    private String stateFile = "target/rag-example-state.json";

    public double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public double getKeywordWeight() {
        return keywordWeight;
    }

    public void setKeywordWeight(double keywordWeight) {
        this.keywordWeight = keywordWeight;
    }

    public String getStateFile() {
        return stateFile;
    }

    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }
}
