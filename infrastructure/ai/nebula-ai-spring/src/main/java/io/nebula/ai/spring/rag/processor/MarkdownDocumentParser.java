package io.nebula.ai.spring.rag.processor;

import io.nebula.ai.spring.rag.model.ParsedDocument;
import io.nebula.ai.spring.rag.model.ParsedDocument.Section;
import io.nebula.ai.spring.rag.model.ParsedDocument.CodeBlock;
import io.nebula.ai.spring.rag.model.ParsedDocument.ConfigExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 文档解析器实现
 * 
 * 解析 Markdown 格式的文档，提取章节、代码块和配置示例
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class MarkdownDocumentParser implements DocumentParser {
    
    private static final Logger log = LoggerFactory.getLogger(MarkdownDocumentParser.class);

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern YAML_CONFIG_PATTERN = Pattern.compile("```ya?ml\\n([\\s\\S]*?)```", Pattern.MULTILINE);
    
    @Override
    public ParsedDocument parse(String markdownContent, String moduleName) {
        ParsedDocument doc = new ParsedDocument();
        doc.setModuleName(moduleName);
        
        log.debug("开始解析模块 {} 的文档，内容长度: {}", moduleName, markdownContent.length());
        
        // 1. 提取标题和章节
        extractSections(markdownContent, doc);
        
        // 2. 提取代码块
        extractCodeBlocks(markdownContent, doc);
        
        // 3. 提取配置示例
        extractConfigExamples(markdownContent, doc);
        
        log.debug("文档解析完成 - 章节数: {}, 代码块数: {}, 配置示例数: {}",
                doc.getSections().size(),
                doc.getCodeBlocks().size(),
                doc.getConfigExamples().size());
        
        return doc;
    }
    
    /**
     * 提取章节
     */
    private void extractSections(String content, ParsedDocument doc) {
        Matcher matcher = HEADING_PATTERN.matcher(content);
        
        Section lastSection = null;
        int lastEnd = 0;
        
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String title = matcher.group(2).trim();
            int start = matcher.start();
            
            // 如果有前一个章节，设置其内容
            if (lastSection != null) {
                String sectionContent = content.substring(lastEnd, start).trim();
                lastSection.setContent(sectionContent);
            }
            
            Section section = new Section(level, title, start);
            doc.addSection(section);
            
            lastSection = section;
            lastEnd = matcher.end();
        }
        
        // 处理最后一个章节
        if (lastSection != null) {
            String sectionContent = content.substring(lastEnd).trim();
            lastSection.setContent(sectionContent);
        }
    }
    
    /**
     * 提取代码块
     */
    private void extractCodeBlocks(String content, ParsedDocument doc) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            
            if (language != null && !language.isEmpty()) {
                // 排除yaml/yml配置块（这些会在extractConfigExamples中处理）
                if (!language.equalsIgnoreCase("yaml") && !language.equalsIgnoreCase("yml")) {
                    CodeBlock codeBlock = new CodeBlock(language, code.trim());
                    doc.addCodeBlock(codeBlock);
                }
            }
        }
    }
    
    /**
     * 提取配置示例（YAML/Properties）
     */
    private void extractConfigExamples(String content, ParsedDocument doc) {
        Matcher yamlMatcher = YAML_CONFIG_PATTERN.matcher(content);
        
        while (yamlMatcher.find()) {
            String yamlContent = yamlMatcher.group(1);
            ConfigExample config = new ConfigExample("yaml", yamlContent.trim());
            doc.addConfigExample(config);
        }
    }
}











