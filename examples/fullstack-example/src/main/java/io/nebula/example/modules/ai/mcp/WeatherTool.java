package io.nebula.example.modules.ai.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.ai.core.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询工具示例
 * 演示如何实现一个MCP工具
 */
@Component
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
public class WeatherTool implements McpTool {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getName() {
        return "get_weather";
    }
    
    @Override
    public String getDescription() {
        return "获取指定城市的天气信息";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> cityProp = new HashMap<>();
        cityProp.put("type", "string");
        cityProp.put("description", "城市名称");
        properties.put("city", cityProp);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"city"});
        
        return schema;
    }
    
    @Override
    public String execute(String arguments) {
        try {
            logger.info("执行天气查询工具,参数: {}", arguments);
            
            Map<String, Object> args = objectMapper.readValue(arguments, Map.class);
            String city = (String) args.get("city");
            
            // 模拟天气数据
            Map<String, Object> weather = new HashMap<>();
            weather.put("city", city);
            weather.put("temperature", 25);
            weather.put("condition", "晴天");
            weather.put("humidity", 60);
            weather.put("windSpeed", 15);
            
            return objectMapper.writeValueAsString(weather);
        } catch (Exception e) {
            logger.error("执行天气查询工具失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}

