package io.nebula.ai.rag.chunking.pack;

import java.util.ArrayList;
import java.util.List;

/**
 * 分隔符层级
 * <p>
 * 超长元素按层级从粗到细递归降级切分：先按空行断段，段还太长就按换行断行，
 * 行还太长就按句末标点断句，句还太长就按空格断词，最后才允许裸字符切。
 * 每往下一级，切出来的边界就更不像人写的自然边界，因此<b>能在上一级解决就绝不下潜</b>。
 * <p>
 * 最后一级是空分隔符，代表字符级硬切。它是兜底而不是常规路径：
 * 一段没有任何自然边界的长文本（例如一整串 base64）只能这么处理。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SeparatorHierarchy {

    /** 字符级硬切的标记：空分隔符 */
    public static final String CHARACTER_LEVEL = "";

    private final List<List<String>> levels;

    /**
     * @param levels 由粗到细的分隔符层级；每层可含多个等价分隔符
     */
    public SeparatorHierarchy(List<List<String>> levels) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("分隔符层级不能为空");
        }
        List<List<String>> copy = new ArrayList<>(levels.size());
        for (List<String> level : levels) {
            if (level == null || level.isEmpty()) {
                throw new IllegalArgumentException("分隔符层级中不允许出现空层");
            }
            copy.add(List.copyOf(level));
        }
        this.levels = List.copyOf(copy);
    }

    /**
     * 中文默认层级：空行 -> 换行 -> 句末标点 -> 空格 -> 字符
     * <p>
     * 句末标点同时收中英文两套：中文技术文档里夹英文句子是常态，
     * 只认中文句号会让英文段落一路降级到按空格切。
     */
    public static SeparatorHierarchy chineseDefault() {
        return new SeparatorHierarchy(List.of(
                List.of("\n\n"),
                List.of("\n"),
                List.of("。", "！", "？", "；", "…", ".", "!", "?", ";"),
                List.of(" "),
                List.of(CHARACTER_LEVEL)));
    }

    /**
     * 层级数量
     */
    public int levelCount() {
        return levels.size();
    }

    /**
     * 取某一层的分隔符
     */
    public List<String> separatorsAt(int level) {
        return levels.get(level);
    }

    /**
     * 该层是否为字符级硬切
     */
    public boolean isCharacterLevel(int level) {
        return levels.get(level).contains(CHARACTER_LEVEL);
    }
}
