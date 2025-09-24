package io.nebula.search.core.model;

import java.util.Map;

public class IndexMapping {
    private Map<String, Object> properties;
    private Map<String, Object> settings;
    
    public IndexMapping() {}
    
    public IndexMapping(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    // Getters and Setters
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public Map<String, Object> getSettings() { return settings; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }
}
