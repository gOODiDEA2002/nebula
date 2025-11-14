package io.nebula.search.core.suggestion;

/**
 * 建议器抽象基类
 * 提供通用的建议器属性和方法
 * 
 * @author nebula
 */
public abstract class AbstractSuggester implements Suggester {
    
    /**
     * 建议器名称
     */
    protected String name;
    
    /**
     * 输入文本
     */
    protected String text;
    
    /**
     * 目标字段
     */
    protected String field;
    
    /**
     * 返回建议数量
     */
    protected Integer size;
    
    protected AbstractSuggester(String name, String text, String field) {
        this.name = name;
        this.text = text;
        this.field = field;
        this.size = 5; // 默认返回5个建议
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getField() {
        return field;
    }
    
    public Integer getSize() {
        return size;
    }
    
    /**
     * 设置返回建议数量
     * 
     * @param size 建议数量
     * @return 当前对象（支持链式调用）
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractSuggester> T size(int size) {
        this.size = size;
        return (T) this;
    }
}

