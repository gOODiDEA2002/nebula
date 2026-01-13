package io.nebula.crawler.core.proxy;

import java.util.List;

/**
 * 代理提供者接口
 * <p>
 * 定义代理获取、报告和管理的核心方法
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface ProxyProvider {

    /**
     * 获取一个可用的代理
     *
     * @return 代理对象，如果没有可用代理返回null
     */
    Proxy getProxy();

    /**
     * 获取多个可用代理
     *
     * @param count 需要的代理数量
     * @return 代理列表
     */
    List<Proxy> getProxies(int count);

    /**
     * 报告代理使用成功
     *
     * @param proxy 成功使用的代理
     */
    void reportSuccess(Proxy proxy);

    /**
     * 报告代理使用失败
     *
     * @param proxy  失败的代理
     * @param reason 失败原因
     */
    void reportFailure(Proxy proxy, String reason);

    /**
     * 获取可用代理数量
     *
     * @return 可用代理数量
     */
    int getAvailableCount();

    /**
     * 刷新代理池
     */
    void refresh();

    /**
     * 清空代理池
     */
    void clear();
}
