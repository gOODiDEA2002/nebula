package io.nebula.crawler.core.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理信息
 * <p>
 * 包含代理的基本信息和使用统计
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proxy {
    
    /**
     * 代理主机地址
     */
    private String host;
    
    /**
     * 代理端口
     */
    private int port;
    
    /**
     * 代理类型
     */
    @Builder.Default
    private ProxyType type = ProxyType.HTTP;
    
    /**
     * 认证用户名
     */
    private String username;
    
    /**
     * 认证密码
     */
    private String password;
    
    /**
     * 是否需要认证
     */
    private boolean authenticated;
    
    /**
     * 响应时间(ms)
     */
    @Builder.Default
    private long responseTime = 0;
    
    /**
     * 成功次数
     */
    @Builder.Default
    private int successCount = 0;
    
    /**
     * 失败次数
     */
    @Builder.Default
    private int failCount = 0;
    
    /**
     * 最后使用时间
     */
    @Builder.Default
    private long lastUseTime = 0;
    
    /**
     * 创建时间
     */
    @Builder.Default
    private long createTime = System.currentTimeMillis();
    
    /**
     * 获取代理地址字符串
     *
     * @return 格式: host:port
     */
    public String toAddress() {
        return host + ":" + port;
    }
    
    /**
     * 获取完整代理URL
     *
     * @return 格式: protocol://host:port
     */
    public String toUrl() {
        String protocol = type == ProxyType.SOCKS5 ? "socks5" :
                          type == ProxyType.SOCKS4 ? "socks4" : "http";
        return protocol + "://" + host + ":" + port;
    }
    
    /**
     * 更新响应时间
     *
     * @param responseTime 响应时间(ms)
     */
    public void updateResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    /**
     * 增加成功计数
     */
    public void incrementSuccess() {
        this.successCount++;
        this.lastUseTime = System.currentTimeMillis();
    }
    
    /**
     * 增加失败计数
     */
    public void incrementFail() {
        this.failCount++;
        this.lastUseTime = System.currentTimeMillis();
    }
    
    /**
     * 获取成功率
     *
     * @return 成功率 (0-1)
     */
    public double getSuccessRate() {
        int total = successCount + failCount;
        if (total == 0) {
            return 1.0;
        }
        return (double) successCount / total;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        this.successCount = 0;
        this.failCount = 0;
        this.responseTime = 0;
    }
    
    /**
     * 从字符串解析代理
     *
     * @param proxyString 格式: host:port 或 host:port:username:password
     * @return Proxy对象
     */
    public static Proxy parse(String proxyString) {
        if (proxyString == null || proxyString.isEmpty()) {
            return null;
        }
        
        String[] parts = proxyString.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        
        Proxy.ProxyBuilder builder = Proxy.builder()
            .host(parts[0])
            .port(Integer.parseInt(parts[1]));
        
        if (parts.length >= 4) {
            builder.username(parts[2])
                   .password(parts[3])
                   .authenticated(true);
        }
        
        return builder.build();
    }
}
