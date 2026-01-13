package io.nebula.ai.spring.rag.processor;

import io.nebula.ai.spring.rag.model.ParsedDocument;

/**
 * 文档解析器接口
 * 
 * 定义文档解析的通用接口，支持不同格式的文档解析实现
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface DocumentParser {
    
    /**
     * 解析文档
     *
     * @param content 文档内容
     * @param moduleName 模块名称（用于标识文档来源）
     * @return 解析后的文档对象
     */
    ParsedDocument parse(String content, String moduleName);
}











