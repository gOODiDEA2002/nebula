package io.nebula.ai.rag.transform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认改写器：trim + 单变体（现状语义）
 */
class TrimQueryTransformerTest {

    private final TrimQueryTransformer transformer = new TrimQueryTransformer();

    @Test
    void trimsWhitespaceAndReturnsSingleVariantWithWeightOne() {
        List<QueryVariant> variants = transformer.transform("  空气开关  ");

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0).getText()).isEqualTo("空气开关");
        assertThat(variants.get(0).getWeight()).isEqualTo(1.0);
    }

    @Test
    void nullQuery_yieldsEmptyTextVariant() {
        List<QueryVariant> variants = transformer.transform(null);

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0).getText()).isEmpty();
    }
}
