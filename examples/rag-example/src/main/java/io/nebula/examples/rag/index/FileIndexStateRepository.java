package io.nebula.examples.rag.index;

import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.core.common.util.JsonUtils;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单 JSON 文件持久化的索引状态库。
 * <p>
 * 用文件实现而非 {@code InMemoryIndexStateRepository}：重启后增量与删除对齐仍成立，
 * 调优教程才能跨重启复现（改 {@code CHUNK_SIZE} 重启后应判「更新」而非「新增」）。
 * <p>
 * 结构：外层 {@code sourceName -> (docId -> DocIndexState)}，load 读全表、save/remove 改后整写。
 * 生产实现应换成 DB/Redis；本示例只求最小可复现。
 *
 * @author Nebula Framework
 */
public class FileIndexStateRepository implements IndexStateRepository {

    private static final TypeReference<Map<String, Map<String, DocIndexState>>> TYPE =
            new TypeReference<>() {
            };

    private final Path file;
    private final Object writeLock = new Object();

    public FileIndexStateRepository(String stateFile) {
        this.file = Path.of(stateFile);
    }

    @Override
    public Map<String, DocIndexState> load(String sourceName) {
        Map<String, Map<String, DocIndexState>> all = readAll();
        Map<String, DocIndexState> byDoc = all.get(sourceName);
        // 返回可变副本，调用方与磁盘状态解耦
        return byDoc == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(byDoc);
    }

    @Override
    public void save(String sourceName, DocIndexState state) {
        synchronized (writeLock) {
            Map<String, Map<String, DocIndexState>> all = readAll();
            all.computeIfAbsent(sourceName, k -> new LinkedHashMap<>())
                    .put(state.getDocId(), state.copy());
            writeAll(all);
        }
    }

    @Override
    public void remove(String sourceName, String docId) {
        synchronized (writeLock) {
            Map<String, Map<String, DocIndexState>> all = readAll();
            Map<String, DocIndexState> byDoc = all.get(sourceName);
            if (byDoc != null) {
                byDoc.remove(docId);
                if (byDoc.isEmpty()) {
                    all.remove(sourceName);
                }
                writeAll(all);
            }
        }
    }

    private Map<String, Map<String, DocIndexState>> readAll() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            Map<String, Map<String, DocIndexState>> parsed = JsonUtils.fromJson(json, TYPE);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (IOException e) {
            throw new IllegalStateException("读取索引状态文件失败: " + file + ", " + e.getMessage(), e);
        }
    }

    private void writeAll(Map<String, Map<String, DocIndexState>> all) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, JsonUtils.toPrettyJson(all), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("写入索引状态文件失败: " + file + ", " + e.getMessage(), e);
        }
    }
}
