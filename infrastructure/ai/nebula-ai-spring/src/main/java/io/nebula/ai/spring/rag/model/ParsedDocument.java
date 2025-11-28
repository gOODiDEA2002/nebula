package io.nebula.ai.spring.rag.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析后的文档对象
 * 
 * 表示从原始文档（如 Markdown）解析出的结构化数据
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class ParsedDocument {
    private String moduleName;
    private String docType; // 文档类型: README, EXAMPLE, CONFIG, etc.
    private String category; // 类别: core, infrastructure, application, etc.
    private String title;
    private List<Section> sections = new ArrayList<>();
    private List<CodeBlock> codeBlocks = new ArrayList<>();
    private List<ConfigExample> configExamples = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();

    public void addSection(Section section) {
        this.sections.add(section);
    }

    public void addCodeBlock(CodeBlock codeBlock) {
        this.codeBlocks.add(codeBlock);
    }

    public void addConfigExample(ConfigExample config) {
        this.configExamples.add(config);
    }

    /**
     * 文档章节
     */
    public static class Section {
        private int level; // 标题级别 (1-6)
        private String title; // 标题文本
        private int startPosition; // 在原文中的起始位置
        private String content; // 章节内容
        private List<Section> subsections = new ArrayList<>(); // 子章节

        public Section() {
        }

        public Section(int level, String title, int startPosition) {
            this.level = level;
            this.title = title;
            this.startPosition = startPosition;
        }
        
        // Getters and Setters
        public int getLevel() {
            return level;
        }
        
        public void setLevel(int level) {
            this.level = level;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public int getStartPosition() {
            return startPosition;
        }
        
        public void setStartPosition(int startPosition) {
            this.startPosition = startPosition;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public List<Section> getSubsections() {
            return subsections;
        }
        
        public void setSubsections(List<Section> subsections) {
            this.subsections = subsections;
        }
    }

    /**
     * 代码块
     */
    public static class CodeBlock {
        private String language; // 编程语言
        private String code; // 代码内容
        private String description; // 描述（从注释提取）
        private List<String> imports; // import语句

        public CodeBlock() {
        }

        public CodeBlock(String language, String code) {
            this.language = language;
            this.code = code;
        }
        
        // Getters and Setters
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public List<String> getImports() {
            return imports;
        }
        
        public void setImports(List<String> imports) {
            this.imports = imports;
        }
    }

    /**
     * 配置示例
     */
    public static class ConfigExample {
        private String format; // yaml/properties
        private String content; // 配置内容
        private Map<String, String> properties; // 解析后的属性

        public ConfigExample() {
        }

        public ConfigExample(String format, String content) {
            this.format = format;
            this.content = content;
        }
        
        // Getters and Setters
        public String getFormat() {
            return format;
        }
        
        public void setFormat(String format) {
            this.format = format;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Map<String, String> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
    
    // Getters and Setters for ParsedDocument
    
    public String getModuleName() {
        return moduleName;
    }
    
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public String getDocType() {
        return docType;
    }
    
    public void setDocType(String docType) {
        this.docType = docType;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<Section> getSections() {
        return sections;
    }
    
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }
    
    public List<CodeBlock> getCodeBlocks() {
        return codeBlocks;
    }
    
    public void setCodeBlocks(List<CodeBlock> codeBlocks) {
        this.codeBlocks = codeBlocks;
    }
    
    public List<ConfigExample> getConfigExamples() {
        return configExamples;
    }
    
    public void setConfigExamples(List<ConfigExample> configExamples) {
        this.configExamples = configExamples;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}



