# Nebula MCP功能测试文档

## 1. 概述

本文档演示Nebula框架的MCP(Model Context Protocol)功能,包括MCP工具和资源的注册查询和调用

## 2. 功能说明

### 2.1 MCP工具(Tools)

MCP工具是可以被AI模型调用的函数工具具有:
- 名称(name): 唯一标识符
- 描述(description): 功能说明
- 输入Schema(inputSchema): 参数定义
- 执行逻辑(execute): 实际功能实现

### 2.2 MCP资源(Resources)

MCP资源是可以被AI模型访问的数据源资源具有:
- URI: 唯一资源标识符
- 名称(name): 显示名称
- 描述(description): 资源说明
- MIME类型(mimeType): 内容类型
- 内容(content): 实际数据

## 3. 配置说明

### 3.1 application.yml配置

```yaml
nebula:
  ai:
    enabled: true
    mcp:
      server:
        enabled: true
        name: "Nebula AI MCP Server"
        version: "1.0.0"
        type: SYNC
        transport: WEBMVC
        sse-message-endpoint: /mcp/message
      client:
        enabled: false
```

配置说明:
- `mcp.server.enabled`: 是否启用MCP服务器
- `mcp.server.name`: 服务器名称
- `mcp.server.type`: 服务器类型(SYNC/ASYNC)
- `mcp.server.transport`: 传输方式(STDIO/WEBMVC/WEBFLUX)

## 4. API测试

### 4.1 获取MCP工具列表

**请求:**
```bash
curl -X GET "http://localhost:8080/api/mcp/tools"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "tools": [
      {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "inputSchema": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名称"
            }
          },
          "required": ["city"]
        }
      }
    ]
  }
}
```

### 4.2 调用MCP工具

**请求:**
```bash
curl -X POST "http://localhost:8080/api/mcp/tools/call" \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "get_weather",
    "arguments": "{\"city\":\"北京\"}"
  }'
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "result": "{\"city\":\"北京\",\"temperature\":25,\"condition\":\"晴天\",\"humidity\":60,\"windSpeed\":15}"
  }
}
```

### 4.3 获取MCP资源列表

**请求:**
```bash
curl -X GET "http://localhost:8080/api/mcp/resources"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "resources": [
      {
        "uri": "file:///docs/readme.md",
        "name": "项目文档",
        "description": "Nebula框架的项目说明文档",
        "mimeType": "text/markdown"
      }
    ]
  }
}
```

### 4.4 读取MCP资源

**请求:**
```bash
curl -X POST "http://localhost:8080/api/mcp/resources/read" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceUri": "file:///docs/readme.md"
  }'
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": "# Nebula框架\n\nNebula是一个基于Spring Boot 3.x和Java 21的企业级后端框架...",
    "mimeType": "text/markdown"
  }
}
```

## 5. 自定义MCP工具示例

### 5.1 实现McpTool接口

```java
@Component
public class MyCustomTool implements McpTool {
    
    @Override
    public String getName() {
        return "my_tool";
    }
    
    @Override
    public String getDescription() {
        return "我的自定义工具";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> paramProp = new HashMap<>();
        paramProp.put("type", "string");
        paramProp.put("description", "参数描述");
        properties.put("param", paramProp);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"param"});
        
        return schema;
    }
    
    @Override
    public String execute(String arguments) {
        // 实现工具逻辑
        return "{\"result\": \"success\"}";
    }
}
```

### 5.2 自动注册

工具使用`@Component`注解后会被自动扫描并注册到MCP服务器,无需额外配置

## 6. 自定义MCP资源示例

### 6.1 实现McpResource接口

```java
@Component
public class MyCustomResource implements McpResource {
    
    @Override
    public String getUri() {
        return "file:///data/mydata.json";
    }
    
    @Override
    public String getName() {
        return "我的数据";
    }
    
    @Override
    public String getDescription() {
        return "自定义数据资源";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
    
    @Override
    public String getContent() {
        return "{\"key\": \"value\"}";
    }
}
```

## 7. 与AI模型集成

MCP工具和资源可以被AI模型自动发现和调用:

```java
@Service
public class AIService {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private McpServerService mcpServerService;
    
    public String chatWithTools(String message) {
        // 获取所有MCP工具
        List<McpTool> tools = mcpServerService.getTools();
        
        // 将工具转换为ToolCallback
        List<ToolCallback> callbacks = tools.stream()
                .map(McpToolAdapter::new)
                .collect(Collectors.toList());
        
        // 使用工具进行聊天
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.userMessage(message)))
                .tools(callbacks)
                .build();
        
        ChatResponse response = chatService.chat(request);
        return response.getContent();
    }
}
```

## 8. Spring AI MCP支持

Spring AI 1.0.3原生支持MCP协议,Nebula框架对其进行了封装:

### 8.1 MCP Server

- 自动注册实现`McpTool`和`McpResource`接口的Bean
- 支持多种传输方式(STDIO, WEBMVC, WEBFLUX)
- 支持同步和异步模式

### 8.2 工具变更通知

当工具或资源发生变更时,MCP服务器会自动通知已连接的客户端

## 9. 常见问题

### 9.1 工具未被注册?

检查:
1. 类是否添加了`@Component`注解
2. 类是否在组件扫描路径下
3. MCP服务器是否已启用(`nebula.ai.mcp.server.enabled=true`)

### 9.2 如何调试工具执行?

在工具的`execute`方法中添加日志:
```java
@Override
public String execute(String arguments) {
    logger.info("执行工具,参数: {}", arguments);
    // 工具逻辑
    return result;
}
```

## 10. 总结

Nebula框架的MCP功能提供了:
- 简单的工具和资源定义接口
- 自动注册机制
- 完整的REST API
- 与Spring AI的无缝集成

这使得开发者可以轻松地为AI模型提供自定义工具和数据访问能力

