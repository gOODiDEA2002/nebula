package io.nebula.example.modules.ai.service.impl;

import io.nebula.ai.core.mcp.McpResource;
import io.nebula.ai.core.mcp.McpServerService;
import io.nebula.ai.core.mcp.McpTool;
import io.nebula.example.modules.ai.entity.dto.McpResourceDto;
import io.nebula.example.modules.ai.entity.dto.McpToolDto;
import io.nebula.example.modules.ai.service.McpDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP演示服务实现
 */
@Service
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
public class McpDemoServiceImpl implements McpDemoService {
    
    private static final Logger logger = LoggerFactory.getLogger(McpDemoServiceImpl.class);
    
    private final McpServerService mcpServerService;
    
    @Autowired
    public McpDemoServiceImpl(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }
    
    @Override
    public McpToolDto.ListResponse listTools(McpToolDto.ListRequest request) {
        logger.info("获取MCP工具列表");
        
        List<McpTool> tools = mcpServerService.getTools();
        
        List<McpToolDto.ToolInfo> toolInfos = tools.stream()
                .map(tool -> {
                    McpToolDto.ToolInfo info = new McpToolDto.ToolInfo();
                    info.setName(tool.getName());
                    info.setDescription(tool.getDescription());
                    info.setInputSchema(tool.getInputSchema());
                    return info;
                })
                .collect(Collectors.toList());
        
        McpToolDto.ListResponse response = new McpToolDto.ListResponse();
        response.setTools(toolInfos);
        
        logger.info("获取到{}个MCP工具", toolInfos.size());
        return response;
    }
    
    @Override
    public McpToolDto.CallResponse callTool(McpToolDto.CallRequest request) {
        logger.info("调用MCP工具: {}, 参数: {}", request.getToolName(), request.getArguments());
        
        List<McpTool> tools = mcpServerService.getTools();
        McpTool tool = tools.stream()
                .filter(t -> t.getName().equals(request.getToolName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("工具不存在: " + request.getToolName()));
        
        String result = tool.execute(request.getArguments());
        
        McpToolDto.CallResponse response = new McpToolDto.CallResponse();
        response.setResult(result);
        
        logger.info("工具执行成功,结果: {}", result);
        return response;
    }
    
    @Override
    public McpResourceDto.ListResponse listResources(McpResourceDto.ListRequest request) {
        logger.info("获取MCP资源列表");
        
        List<McpResource> resources = mcpServerService.getResources();
        
        List<McpResourceDto.ResourceInfo> resourceInfos = resources.stream()
                .map(resource -> {
                    McpResourceDto.ResourceInfo info = new McpResourceDto.ResourceInfo();
                    info.setUri(resource.getUri());
                    info.setName(resource.getName());
                    info.setDescription(resource.getDescription());
                    info.setMimeType(resource.getMimeType());
                    return info;
                })
                .collect(Collectors.toList());
        
        McpResourceDto.ListResponse response = new McpResourceDto.ListResponse();
        response.setResources(resourceInfos);
        
        logger.info("获取到{}个MCP资源", resourceInfos.size());
        return response;
    }
    
    @Override
    public McpResourceDto.ReadResponse readResource(McpResourceDto.ReadRequest request) {
        logger.info("读取MCP资源: {}", request.getResourceUri());
        
        List<McpResource> resources = mcpServerService.getResources();
        McpResource resource = resources.stream()
                .filter(r -> r.getUri().equals(request.getResourceUri()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("资源不存在: " + request.getResourceUri()));
        
        String content = resource.getContent();
        
        McpResourceDto.ReadResponse response = new McpResourceDto.ReadResponse();
        response.setContent(content);
        response.setMimeType(resource.getMimeType());
        
        logger.info("资源读取成功,内容长度: {}", content.length());
        return response;
    }
}

