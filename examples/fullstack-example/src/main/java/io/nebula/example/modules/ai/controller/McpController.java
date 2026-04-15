package io.nebula.example.modules.ai.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.ai.entity.dto.McpResourceDto;
import io.nebula.example.modules.ai.entity.dto.McpToolDto;
import io.nebula.example.modules.ai.service.McpDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * MCP功能演示Controller
 */
@Tag(name = "MCP演示", description = "Model Context Protocol功能演示接口")
@RestController
@RequestMapping("/api/mcp")
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
public class McpController {
    
    private final McpDemoService mcpDemoService;
    
    @Autowired
    public McpController(McpDemoService mcpDemoService) {
        this.mcpDemoService = mcpDemoService;
    }
    
    @Operation(summary = "获取MCP工具列表", description = "获取所有注册的MCP工具")
    @GetMapping("/tools")
    public Result<McpToolDto.ListResponse> listTools() {
        McpToolDto.ListRequest request = new McpToolDto.ListRequest();
        McpToolDto.ListResponse response = mcpDemoService.listTools(request);
        return Result.success(response);
    }
    
    @Operation(summary = "调用MCP工具", description = "执行指定的MCP工具")
    @PostMapping("/tools/call")
    public Result<McpToolDto.CallResponse> callTool(@Valid @RequestBody McpToolDto.CallRequest request) {
        McpToolDto.CallResponse response = mcpDemoService.callTool(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取MCP资源列表", description = "获取所有注册的MCP资源")
    @GetMapping("/resources")
    public Result<McpResourceDto.ListResponse> listResources() {
        McpResourceDto.ListRequest request = new McpResourceDto.ListRequest();
        McpResourceDto.ListResponse response = mcpDemoService.listResources(request);
        return Result.success(response);
    }
    
    @Operation(summary = "读取MCP资源", description = "读取指定的MCP资源内容")
    @PostMapping("/resources/read")
    public Result<McpResourceDto.ReadResponse> readResource(@Valid @RequestBody McpResourceDto.ReadRequest request) {
        McpResourceDto.ReadResponse response = mcpDemoService.readResource(request);
        return Result.success(response);
    }
}

