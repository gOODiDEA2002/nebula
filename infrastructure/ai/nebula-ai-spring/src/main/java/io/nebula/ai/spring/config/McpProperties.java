package io.nebula.ai.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP配置属性
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
         * 传输方式: STDIO, WEBMVC, WEBFLUX
         */
        private String transport = "STDIO";
        
        /**
         * SSE消息端点(用于WEBMVC和WEBFLUX)
         */
        private String sseMessageEndpoint = "/mcp/message";
        
        /**
         * 是否启用工具变更通知
         */
        private boolean toolChangeNotification = true;
        
        /**
         * 是否启用资源变更通知
         */
        private boolean resourceChangeNotification = true;
        
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
        
        public String getTransport() {
            return transport;
        }
        
        public void setTransport(String transport) {
            this.transport = transport;
        }
        
        public String getSseMessageEndpoint() {
            return sseMessageEndpoint;
        }
        
        public void setSseMessageEndpoint(String sseMessageEndpoint) {
            this.sseMessageEndpoint = sseMessageEndpoint;
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

