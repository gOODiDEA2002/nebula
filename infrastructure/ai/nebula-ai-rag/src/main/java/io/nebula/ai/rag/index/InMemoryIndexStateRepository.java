package io.nebula.ai.rag.index;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存索引状态库
 * <p>
 * <b>仅测试与单次任务用，不进任何自动装配（上位 DS6）。</b>重启即失忆：进程一停，
 * 「哪些文档已索引」的记录全部丢失，因此它<b>无法支撑删除对齐</b>（下一次快照少了某文档时，
 * 没有历史状态就不知道该删谁）。持续增量场景必须换持久化实现（DB/Redis/文件）。
 * <p>
 * 每次 {@code save} 存一份深拷贝，模拟真实持久化的「序列化即快照」，避免调用方后续 mutate
 * 回写已存的进度 —— 这正是重入语义得以验证的前提。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class InMemoryIndexStateRepository implements IndexStateRepository {

    private final Map<String, Map<String, DocIndexState>> store = new ConcurrentHashMap<>();

    @Override
    public Map<String, DocIndexState> load(String sourceName) {
        Map<String, DocIndexState> states = store.get(sourceName);
        if (states == null) {
            return new LinkedHashMap<>();
        }
        Map<String, DocIndexState> copy = new LinkedHashMap<>();
        for (Map.Entry<String, DocIndexState> entry : states.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @Override
    public void save(String sourceName, DocIndexState state) {
        store.computeIfAbsent(sourceName, k -> new ConcurrentHashMap<>())
                .put(state.getDocId(), state.copy());
    }

    @Override
    public void remove(String sourceName, String docId) {
        Map<String, DocIndexState> states = store.get(sourceName);
        if (states != null) {
            states.remove(docId);
        }
    }
}
