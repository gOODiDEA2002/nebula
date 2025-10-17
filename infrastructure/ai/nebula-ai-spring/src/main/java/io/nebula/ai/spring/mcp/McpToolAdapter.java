package io.nebula.ai.spring.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.ai.core.mcp.McpTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * MCP工具适配器
 * 将Nebula的McpTool适配为Spring AI的ToolCallback
 */
public class McpToolAdapter implements ToolCallback {
    
    private final McpTool mcpTool;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;
    
    public McpToolAdapter(McpTool mcpTool) {
        this.mcpTool = mcpTool;
        this.objectMapper = new ObjectMapper();
        this.toolDefinition = createToolDefinition();
    }
    
    private ToolDefinition createToolDefinition() {
        try {
            Map<String, Object> inputSchema = mcpTool.getInputSchema();
            String schemaJson = objectMapper.writeValueAsString(inputSchema);
            
            return ToolDefinition.builder()
                    .name(mcpTool.getName())
                    .description(mcpTool.getDescription())
                    .inputSchema(schemaJson)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tool definition for: " + mcpTool.getName(), e);
        }
    }
    
    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }
    
    @Override
    public String call(String toolInput) {
        return mcpTool.execute(toolInput);
    }
}

