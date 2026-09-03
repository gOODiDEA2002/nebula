package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code assembleDetailed} 与 {@code assemble} 的行为保持与序号映射契约（R4 §4.2、§10）
 */
class ContextAssemblyTest {

    private static final String TEMPLATE = "[文档%d]%s\n";

    private static RetrievalResult doc(String id, String content) {
        return RetrievalResult.builder()
                .id(id).content(content).metadata(Map.of()).score(1.0).source("vector").build();
    }

    @Test
    void assemble_delegatesToAssembleDetailed_byteIdentical() {
        ContextAssembler assembler = new ContextAssembler(100, TEMPLATE);
        List<List<RetrievalResult>> inputs = List.of(
                List.of(),
                List.of(doc("a", "内容一")),
                List.of(doc("a", "内容一"), doc("b", "内容二"), doc("c", "内容三")));

        for (List<RetrievalResult> input : inputs) {
            assertThat(assembler.assemble(input))
                    .isEqualTo(assembler.assembleDetailed(input).getContext());
        }
    }

    @Test
    void emptyList_yieldsEmptyContextAndNoReferences() {
        ContextAssembler assembler = new ContextAssembler(100, TEMPLATE);

        ContextAssembly assembly = assembler.assembleDetailed(List.of());

        assertThat(assembly.getContext()).isEmpty();
        assertThat(assembly.getIncludedReferences()).isEmpty();
        assertThat(assembly.getCitationMap()).isEmpty();
        assertThat(assembly.getOmittedCount()).isZero();
    }

    @Test
    void allFit_citationMapIsOneBasedInOrder() {
        ContextAssembler assembler = new ContextAssembler(1000, TEMPLATE);

        ContextAssembly assembly = assembler.assembleDetailed(
                List.of(doc("a", "内容一"), doc("b", "内容二")));

        assertThat(assembly.getIncludedReferences()).extracting(RetrievalResult::getId).containsExactly("a", "b");
        assertThat(assembly.getCitationMap()).hasSize(2);
        assertThat(assembly.getCitationMap().get(1).getId()).isEqualTo("a");
        assertThat(assembly.getCitationMap().get(2).getId()).isEqualTo("b");
        assertThat(assembly.getOmittedCount()).isZero();
    }

    @Test
    void budgetOverflow_omitsTailAndCountsOmitted() {
        // 每篇约 8-9 字符（"[文档1]内容一\n"），预算只容得下前两篇
        String longContent = "非常长的内容".repeat(10);
        ContextAssembler assembler = new ContextAssembler(30, TEMPLATE);

        ContextAssembly assembly = assembler.assembleDetailed(
                List.of(doc("a", "短一"), doc("b", "短二"), doc("c", longContent)));

        assertThat(assembly.getIncludedReferences()).extracting(RetrievalResult::getId).containsExactly("a", "b");
        assertThat(assembly.getOmittedCount()).isEqualTo(1);
        assertThat(assembly.getContext()).isEqualTo(assembler.assemble(
                List.of(doc("a", "短一"), doc("b", "短二"), doc("c", longContent))));
    }

    @Test
    void singleOverBudget_yieldsEmptyContextAllOmitted() {
        ContextAssembler assembler = new ContextAssembler(5, TEMPLATE);

        ContextAssembly assembly = assembler.assembleDetailed(List.of(doc("a", "这是一段超过预算的内容")));

        assertThat(assembly.getContext()).isEmpty();
        assertThat(assembly.getIncludedReferences()).isEmpty();
        assertThat(assembly.getOmittedCount()).isEqualTo(1);
    }

    @Test
    void noopCitationPostProcessor_returnsAnswerUnchanged() {
        NoopCitationPostProcessor processor = new NoopCitationPostProcessor();
        ContextAssembly assembly = new ContextAssembler(100, TEMPLATE)
                .assembleDetailed(List.of(doc("a", "内容")));

        assertThat(processor.process("原始答案", assembly)).isEqualTo("原始答案");
    }

    @Test
    void citationMapAndReferences_areImmutable() {
        ContextAssembly assembly = new ContextAssembler(1000, TEMPLATE)
                .assembleDetailed(List.of(doc("a", "内容一")));

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> assembly.getIncludedReferences().add(doc("x", "y")));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> assembly.getCitationMap().put(9, doc("x", "y")));
    }
}
