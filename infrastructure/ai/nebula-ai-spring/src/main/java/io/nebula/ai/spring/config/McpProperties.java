package io.nebula.ai.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP配置属性
 * 
 * 对齐 Spring AI 1.1.0 MCP Server Starter 配置
 * 
 * 用户可以使用 nebula.ai.mcp 配置，框架会自动桥接到 Spring AI 配置
 * 
 * @see org.springframework.ai.mcp.spring.autoconfigure.server.McpServerProperties
 */
@ConfigurationProperties(prefix = "nebula.ai.mcp")
public class McpProperties {
    
    /**
     * MCP服务器配置
     */
    private Server server = new Server();
    
    /**
     * MCP客户端配置
     */
    private Client client = new Client();
    
    public Server getServer() {
        return server;
    }
    
    public void setServer(Server server) {
        this.server = server;
    }
    
    public Client getClient() {
        return client;
    }
    
    public void setClient(Client client) {
        this.client = client;
    }
    
    /**
     * MCP服务器配置
     * 对齐 Spring AI 1.1.0 McpServerProperties
     */
    public static class Server {
        /**
         * 是否启用MCP服务器
         */
        private boolean enabled = false;
        
        /**
         * 服务器名称
         */
        private String name = "Nebula AI MCP Server";
        
        /**
         * 服务器版本
         */
        private String version = "1.0.0";
        
        /**
         * 服务器类型: SYNC(同步) 或 ASYNC(异步)
         */
        private String type = "SYNC";
        
        /**
         * 协议类型: STREAMABLE, STDIO
         * STREAMABLE 用于 HTTP 传输（推荐）
         * STDIO 用于标准输入输出
         */
        private String protocol = "STREAMABLE";
        
        /**
         * 服务说明（向客户端暴露的描述信息）
         */
        private String instructions = "Nebula Framework AI服务";
        
        /**
         * 是否启用工具回调转换器
         * 开启后会自动检测 @Tool 注解的方法
         */
        private boolean toolCallbackConverter = true;
        
        /**
         * 能力配置
         */
        private Capabilities capabilities = new Capabilities();
        
        /**
         * Streamable HTTP 配置
         */
        private StreamableHttp streamableHttp = new StreamableHttp();
        
        /**
         * 是否启用工具变更通知
         */
        private boolean toolChangeNotification = false;
        
        /**
         * 是否启用资源变更通知
         */
        private boolean resourceChangeNotification = false;
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getProtocol() {
            return protocol;
        }
        
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
        
        public String getInstructions() {
            return instructions;
        }
        
        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
        
        public boolean isToolCallbackConverter() {
            return toolCallbackConverter;
        }
        
        public void setToolCallbackConverter(boolean toolCallbackConverter) {
            this.toolCallbackConverter = toolCallbackConverter;
        }
        
        public Capabilities getCapabilities() {
            return capabilities;
        }
        
        public void setCapabilities(Capabilities capabilities) {
            this.capabilities = capabilities;
        }
        
        public StreamableHttp getStreamableHttp() {
            return streamableHttp;
        }
        
        public void setStreamableHttp(StreamableHttp streamableHttp) {
            this.streamableHttp = streamableHttp;
        }
        
        public boolean isToolChangeNotification() {
            return toolChangeNotification;
        }
        
        public void setToolChangeNotification(boolean toolChangeNotification) {
            this.toolChangeNotification = toolChangeNotification;
        }
        
        public boolean isResourceChangeNotification() {
            return resourceChangeNotification;
        }
        
        public void setResourceChangeNotification(boolean resourceChangeNotification) {
            this.resourceChangeNotification = resourceChangeNotification;
        }
    }
    
    /**
     * MCP能力配置
     */
    public static class Capabilities {
        /**
         * 是否支持工具调用
         */
        private boolean tool = true;
        
        /**
         * 是否支持资源访问
         */
        private boolean resource = true;
        
        /**
         * 是否支持 prompt 模板
         */
        private boolean prompt = false;
        
        /**
         * 是否支持补全
         */
        private boolean completion = false;
        
        public boolean isTool() {
            return tool;
        }
        
        public void setTool(boolean tool) {
            this.tool = tool;
        }
        
        public boolean isResource() {
            return resource;
        }
        
        public void setResource(boolean resource) {
            this.resource = resource;
        }
        
        public boolean isPrompt() {
            return prompt;
        }
        
        public void setPrompt(boolean prompt) {
            this.prompt = prompt;
        }
        
        public boolean isCompletion() {
            return completion;
        }
        
        public void setCompletion(boolean completion) {
            this.completion = completion;
        }
    }
    
    /**
     * Streamable HTTP 配置
     */
    public static class StreamableHttp {
        /**
         * MCP 协议端点
         */
        private String mcpEndpoint = "/mcp";
        
        /**
         * 保持连接的心跳间隔
         */
        private String keepAliveInterval = "30s";
        
        public String getMcpEndpoint() {
            return mcpEndpoint;
        }
        
        public void setMcpEndpoint(String mcpEndpoint) {
            this.mcpEndpoint = mcpEndpoint;
        }
        
        public String getKeepAliveInterval() {
            return keepAliveInterval;
        }
        
        public void setKeepAliveInterval(String keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
        }
    }
    
    /**
     * MCP客户端配置
     */
    public static class Client {
        /**
         * 是否启用MCP客户端
         */
        private boolean enabled = false;
        
        /**
         * 服务器连接URL(用于HTTP传输)
         */
        private String serverUrl;
        
        /**
         * 请求超时时间(秒)
         */
        private int requestTimeout = 30;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getServerUrl() {
            return serverUrl;
        }
        
        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }
        
        public int getRequestTimeout() {
            return requestTimeout;
        }
        
        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }
}

