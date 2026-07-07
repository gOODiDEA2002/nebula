package io.nebula.data.cache.sync;

/**
 * 多级缓存跨节点失效广播接口（CD-10）
 *
 * <p>多级缓存的 L1 是各节点私有的进程内缓存: 节点 A 更新/删除某 key 后,
 * 其他节点的 L1 副本在自身 TTL 到期前会持续返回旧值。本接口用于在写路径上
 * 向其他节点广播失效通知, 收到通知的节点驱逐本地 L1 对应条目, 下次读取回源 L2。</p>
 *
 * <p>广播是尽力而为的优化: 发布失败不影响本地写入结果, L1 自身的短 TTL 仍是
 * 最终一致性兜底。</p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public interface CacheInvalidationBroadcaster {

    /**
     * 广播单个 key 失效, 通知其他节点驱逐各自 L1 中的该条目（不影响本节点）
     *
     * @param key 缓存键（逻辑键, 不含 L2 存储前缀）
     */
    void publishEvict(String key);

    /**
     * 广播全量清空, 通知其他节点清空各自的 L1（不影响本节点）
     */
    void publishClear();
}
