package io.nebula.crawler.proxy.provider;

import io.nebula.crawler.core.proxy.Proxy;

import java.util.List;

/**
 * 代理来源接口
 * <p>
 * 定义从不同来源获取代理的方法
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface ProxySource {

    /**
     * 获取代理来源名称
     *
     * @return 来源名称
     */
    String getName();

    /**
     * 获取代理来源类型
     *
     * @return 来源类型字符串
     */
    String getType();

    /**
     * 获取代理列表
     *
     * @return 代理列表
     */
    List<Proxy> fetch();
    
    /**
     * 获取代理来源优先级
     * 数值越小优先级越高
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 是否启用
     *
     * @return true表示启用
     */
    default boolean isEnabled() {
        return true;
    }
}
