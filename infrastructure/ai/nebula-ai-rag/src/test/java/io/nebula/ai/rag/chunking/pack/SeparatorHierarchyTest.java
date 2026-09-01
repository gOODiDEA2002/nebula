package io.nebula.ai.rag.chunking.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分隔符层级与按分隔符切分
 * <p>
 * 关键不变量：分隔符留在前一段末尾，因此把所有段拼回去必须逐字还原原文。
 * 切分丢字符是最难发现的一类缺陷 —— 块看起来都正常，只是内容少了一点。
 */
@DisplayName("SeparatorHierarchy 与切分工具")
class SeparatorHierarchyTest {

    @Test
    @DisplayName("中文默认层级: 空行 -> 换行 -> 句末标点 -> 空格 -> 字符")
    void chineseDefault_hasFiveLevels() {
        SeparatorHierarchy hierarchy = SeparatorHierarchy.chineseDefault();

        assertThat(hierarchy.levelCount()).isEqualTo(5);
        assertThat(hierarchy.separatorsAt(0)).containsExactly("\n\n");
        assertThat(hierarchy.separatorsAt(1)).containsExactly("\n");
        assertThat(hierarchy.separatorsAt(2)).contains("。", "？", ".", "?");
        assertThat(hierarchy.separatorsAt(3)).containsExactly(" ");
        assertThat(hierarchy.isCharacterLevel(4)).isTrue();
        assertThat(hierarchy.isCharacterLevel(0)).isFalse();
    }

    @Test
    @DisplayName("空层级或空的某一层构造期拒绝")
    void invalidLevels_failFast() {
        assertThatThrownBy(() -> new SeparatorHierarchy(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeparatorHierarchy(List.of(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("按分隔符切分后拼接可逐字还原原文")
    void splitKeepingSeparator_isLossless() {
        String text = "第一句。第二句！第三句？收尾";

        List<String> segments = ChunkPacker.splitKeepingSeparator(
                text, List.of("。", "！", "？"));

        assertThat(segments).containsExactly("第一句。", "第二句！", "第三句？", "收尾");
        assertThat(String.join("", segments)).isEqualTo(text);
    }

    @Test
    @DisplayName("分隔符不存在时返回整段, 交由上层继续下潜")
    void missingSeparator_yieldsSingleSegment() {
        List<String> segments = ChunkPacker.splitKeepingSeparator("没有任何标点", List.of("。"));

        assertThat(segments).containsExactly("没有任何标点");
    }

    @Test
    @DisplayName("多字符分隔符按整体匹配")
    void multiCharSeparator_isMatchedAsWhole() {
        List<String> segments = ChunkPacker.splitKeepingSeparator(
                "甲段\n\n乙段\n\n丙段", List.of("\n\n"));

        assertThat(segments).containsExactly("甲段\n\n", "乙段\n\n", "丙段");
    }
}
