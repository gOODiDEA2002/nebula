package io.nebula.search.core.model;

import java.util.Map;

public class IndexInfo {
    private String indexName;
    private Map<String, Object> mappings;
    private Map<String, Object> settings;
    private long documentCount;
    private long storeSize;
    
    public IndexInfo() {}
    
    // Getters and Setters
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    
    public Map<String, Object> getMappings() { return mappings; }
    public void setMappings(Map<String, Object> mappings) { this.mappings = mappings; }
    
    public Map<String, Object> getSettings() { return settings; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }
    
    public long getDocumentCount() { return documentCount; }
    public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }
    
    public long getStoreSize() { return storeSize; }
    public void setStoreSize(long storeSize) { this.storeSize = storeSize; }
}
