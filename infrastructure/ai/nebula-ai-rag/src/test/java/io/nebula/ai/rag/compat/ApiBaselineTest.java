package io.nebula.ai.rag.compat;

import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.chunking.TextChunker;
import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.pipeline.AnswerGenerator;
import io.nebula.ai.rag.pipeline.ContextAssembler;
import io.nebula.ai.rag.pipeline.DefaultRagPipeline;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.pipeline.RagAnswer;
import io.nebula.ai.rag.pipeline.RagPipeline;
import io.nebula.ai.rag.pipeline.RagPromptRenderer;
import io.nebula.ai.rag.pipeline.RagQuery;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * API 兼容红线门禁（详细设计 §1）
 * <p>
 * <b>它守的是什么：</b>SIA（source-insight-ai）生产在线消费 {@code 2.1.1-SNAPSHOT} 且 CI 带 {@code -U}，
 * 本框架每次提交都会被下游立刻拾取，因此只允许兼容增量。本类把 SIA 实际消费到的 API 面
 * （2026-08-30 经 SIA 源码检索得出）用反射逐项钉死：类存在、成员存在、参数与返回类型精确匹配、
 * 可被继承/实现的成员保持可覆盖。任何人改动红线成员，本模块测试期立刻爆红。
 * <p>
 * <b>维护规则（改动本类前必读）：</b>
 * <ol>
 *   <li>本类的清单与 {@code docs/design/rag-optimization-r1-detailed-design.md} §1 表格同源。
 *       两边必须一起改，只改一边等于门禁失效。</li>
 *   <li><b>SIA 侧新增对本模块的消费面（新的实现/继承/Bean 注入/构造调用/常量判等）时，
 *       必须同步在本类补上对应断言</b>，否则该消费面不受门禁保护。</li>
 *   <li>删除或放宽本类的任何断言，等于宣布「允许破坏 SIA」，必须先完成跨仓协调：
 *       发布独立版本坐标（如 {@code 2.2.0-SNAPSHOT}）并让 SIA 显式升级，
 *       而不是直接把断言删掉让构建变绿。</li>
 *   <li>新增能力一律走「新增类 / 新增重载 / default 方法」，不改动本类已钉住的签名。</li>
 * </ol>
 * 断言失败时的排查入口就是本注释与详细设计 §1，不要先怀疑测试写错了。
 */
@DisplayName("API 兼容红线: SIA 消费面基线")
class ApiBaselineTest {

    // ------------------------------------------------------------------
    // 第 1 类：被 SIA 业务类实现的接口
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("1. 接口: SIA 业务类实现的四个接口")
    class InterfaceContracts {

        @Test
        @DisplayName("Retriever: retrieve / getName / getWeight 抽象, timeoutMillis 为 default")
        void retriever_keepsFourMembers() {
            assertInterface(Retriever.class);

            Method retrieve = assertPublicMethod(Retriever.class, "retrieve", List.class,
                    String.class, int.class, Map.class);
            assertAbstract(retrieve);
            assertAbstract(assertPublicMethod(Retriever.class, "getName", String.class));
            assertAbstract(assertPublicMethod(Retriever.class, "getWeight", double.class));

            // timeoutMillis 必须是 default 方法: SIA 的 KeywordRetriever / GraphRetriever /
            // VectorRetriever 都没有实现它, 改成抽象方法会让三个业务类直接编译失败
            Method timeout = assertPublicMethod(Retriever.class, "timeoutMillis", long.class);
            assertThat(timeout.isDefault())
                    .as("Retriever.timeoutMillis 必须保持 default 实现(详细设计 §1)")
                    .isTrue();
        }

        @Test
        @DisplayName("Reranker: rerank / getName 抽象")
        void reranker_keepsTwoMembers() {
            assertInterface(Reranker.class);
            assertAbstract(assertPublicMethod(Reranker.class, "rerank", List.class,
                    String.class, List.class, int.class));
            assertAbstract(assertPublicMethod(Reranker.class, "getName", String.class));
        }

        @Test
        @DisplayName("AnswerGenerator: 保持单抽象方法(SIA 以 lambda 提供实现)")
        void answerGenerator_staysFunctional() {
            assertInterface(AnswerGenerator.class);
            assertPublicMethod(AnswerGenerator.class, "generate", String.class,
                    String.class, long.class);
            assertSingleAbstractMethod(AnswerGenerator.class);
        }

        @Test
        @DisplayName("RagPromptRenderer: 保持单抽象方法(SIA 以 lambda 提供实现)")
        void ragPromptRenderer_staysFunctional() {
            assertInterface(RagPromptRenderer.class);
            assertPublicMethod(RagPromptRenderer.class, "render", String.class,
                    String.class, String.class);
            assertSingleAbstractMethod(RagPromptRenderer.class);
        }

        @Test
        @DisplayName("RagPipeline: query(RagQuery) 抽象(SIA 注入并委托)")
        void ragPipeline_keepsQueryMethod() {
            assertInterface(RagPipeline.class);
            assertAbstract(assertPublicMethod(RagPipeline.class, "query", RagAnswer.class,
                    RagQuery.class));
        }
    }

    // ------------------------------------------------------------------
    // 第 2 类：被 SIA 具体类继承的类
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("2. 具体类继承: SIA VectorRetriever extends VectorStoreRetriever")
    class InheritanceSurface {

        @Test
        @DisplayName("四参构造器 (VectorStoreService, String, double, double) 存在且 public")
        void fourArgConstructor_isPreserved() {
            // SIA VectorRetriever 的 super(...) 调用逐参对齐这个描述符
            Constructor<?> ctor = assertPublicConstructor(VectorStoreRetriever.class,
                    VectorStoreService.class, String.class, double.class, double.class);
            assertThat(ctor.getParameterCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("三参构造器 (VectorStoreService, double, double) 存在(框架自动装配在用)")
        void threeArgConstructor_isPreserved() {
            assertPublicConstructor(VectorStoreRetriever.class,
                    VectorStoreService.class, double.class, double.class);
        }

        @Test
        @DisplayName("类可继承, retrieve / getName / getWeight 可覆盖")
        void classAndMembers_stayOverridable() {
            assertThat(Modifier.isFinal(VectorStoreRetriever.class.getModifiers()))
                    .as("VectorStoreRetriever 不能是 final: SIA VectorRetriever 继承它")
                    .isFalse();
            assertThat(Modifier.isPublic(VectorStoreRetriever.class.getModifiers())).isTrue();

            assertOverridable(assertPublicMethod(VectorStoreRetriever.class, "retrieve",
                    List.class, String.class, int.class, Map.class));
            assertOverridable(assertPublicMethod(VectorStoreRetriever.class, "getName", String.class));
            assertOverridable(assertPublicMethod(VectorStoreRetriever.class, "getWeight", double.class));
        }

        @Test
        @DisplayName("SOURCE 常量值为 vector(融合按 source 识别来源)")
        void sourceConstant_isPreserved() {
            assertConstant(VectorStoreRetriever.class, "SOURCE", String.class, "vector");
        }
    }

    // ------------------------------------------------------------------
    // 第 3 类：被 SIA 以 Bean 类型 getBean / 注入 / instanceof / cast 使用的类型
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("3. Bean 类型: SIA 按类型取 Bean 或做 instanceof/cast")
    class BeanTypes {

        @Test
        @DisplayName("DefaultRagPipeline 是 RagPipeline 的实现(SIA 断言 isInstanceOf)")
        void defaultRagPipeline_implementsRagPipeline() {
            assertThat(RagPipeline.class.isAssignableFrom(DefaultRagPipeline.class))
                    .as("SIA RagPipelineWiringTest 断言容器里的 RagPipeline 是 DefaultRagPipeline")
                    .isTrue();
            assertThat(Modifier.isPublic(DefaultRagPipeline.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("HybridRetrievalEngine 可按类型取 Bean, getRetrievers 返回检索器列表")
        void hybridRetrievalEngine_keepsPublicSurface() {
            assertThat(Modifier.isPublic(HybridRetrievalEngine.class.getModifiers())).isTrue();
            assertPublicMethod(HybridRetrievalEngine.class, "getRetrievers", List.class);
            assertPublicMethod(HybridRetrievalEngine.class, "retrieve", List.class,
                    String.class, int.class, Map.class);
        }

        @Test
        @DisplayName("RrfFusionStrategy 实现 FusionStrategy, getRrfK / getSourcePriority 可读")
        void rrfFusionStrategy_keepsAccessors() {
            assertThat(FusionStrategy.class.isAssignableFrom(RrfFusionStrategy.class))
                    .as("SIA 把容器里的 FusionStrategy 强转成 RrfFusionStrategy")
                    .isTrue();
            assertPublicMethod(RrfFusionStrategy.class, "getRrfK", int.class);
            assertPublicMethod(RrfFusionStrategy.class, "getSourcePriority", List.class);
            assertPublicConstructor(RrfFusionStrategy.class, int.class, List.class);
            assertPublicConstructor(RrfFusionStrategy.class);
        }

        @Test
        @DisplayName("TextChunker 可按类型取 Bean, chunk(String) 与 chunk(String,int,int) 均在")
        void textChunker_keepsChunkMethods() {
            assertThat(Modifier.isPublic(TextChunker.class.getModifiers())).isTrue();
            // SIA KnowledgeIndexer 调 chunk(content) 单参重载
            assertPublicMethod(TextChunker.class, "chunk", List.class, String.class);
            assertPublicMethod(TextChunker.class, "chunk", List.class,
                    String.class, int.class, int.class);
            assertPublicMethod(TextChunker.class, "chunkByParagraph", List.class,
                    String.class, int.class);
        }
    }

    // ------------------------------------------------------------------
    // 第 4 类：被 SIA 判等的常量（String 常量会在 SIA 编译期内联，值本身也是红线）
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("4. 常量: SIA 用于判等的降级原因")
    class Constants {

        @Test
        @DisplayName("REASON_NO_DOCUMENT 值为 no-document")
        void reasonNoDocument_keepsValue() {
            // SIA RagPipelineImpl 用它判断「一条都没检索到」并走不计费分支;
            // 值被 javac 内联进 SIA 字节码, 因此改值等于静默破坏下游判断
            assertConstant(DefaultRagPipeline.class, "REASON_NO_DOCUMENT", String.class, "no-document");
        }

        @Test
        @DisplayName("REASON_GENERATION_TIMEOUT 值为 generation-timeout")
        void reasonGenerationTimeout_keepsValue() {
            assertConstant(DefaultRagPipeline.class, "REASON_GENERATION_TIMEOUT", String.class,
                    "generation-timeout");
        }
    }

    // ------------------------------------------------------------------
    // 第 5 类：被 SIA 构造与读取的模型
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("5. 模型: builder 与 getter")
    class ModelTypes {

        @Test
        @DisplayName("RetrievalResult: builder 五个属性 + 全套 getter")
        void retrievalResult_keepsBuilderAndGetters() {
            assertBuilder(RetrievalResult.class, "id", "content", "metadata", "score", "source");
            assertPublicMethod(RetrievalResult.class, "getId", String.class);
            assertPublicMethod(RetrievalResult.class, "getContent", String.class);
            assertPublicMethod(RetrievalResult.class, "getMetadata", Map.class);
            assertPublicMethod(RetrievalResult.class, "getScore", double.class);
            assertPublicMethod(RetrievalResult.class, "getSource", String.class);
        }

        @Test
        @DisplayName("RagQuery: builder 五个属性(SIA 逐个调用)")
        void ragQuery_keepsBuilder() {
            assertBuilder(RagQuery.class, "query", "topK", "filter", "enableRerank", "generateAnswer");
            assertPublicMethod(RagQuery.class, "of", RagQuery.class, String.class);
            assertPublicMethod(RagQuery.class, "getQuery", String.class);
            assertPublicMethod(RagQuery.class, "getTopK", Integer.class);
            assertPublicMethod(RagQuery.class, "getFilter", Map.class);
            assertPublicMethod(RagQuery.class, "getEnableRerank", Boolean.class);
            assertPublicMethod(RagQuery.class, "isGenerateAnswer", boolean.class);
        }

        @Test
        @DisplayName("RagAnswer: builder + getAnswer/getReferences/getDegradeReason/isDegraded")
        void ragAnswer_keepsBuilderAndGetters() {
            assertBuilder(RagAnswer.class, "answer", "references", "costMs", "degraded", "degradeReason");
            assertPublicMethod(RagAnswer.class, "getAnswer", String.class);
            assertPublicMethod(RagAnswer.class, "getReferences", List.class);
            assertPublicMethod(RagAnswer.class, "getCostMs", long.class);
            assertPublicMethod(RagAnswer.class, "isDegraded", boolean.class);
            assertPublicMethod(RagAnswer.class, "getDegradeReason", String.class);
        }
    }

    // ------------------------------------------------------------------
    // 第 6 类：被 Bean 定义与测试直接调用的构造器
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("6. 构造器: 自动装配与 SIA 测试直接 new 的描述符")
    class Constructors {

        @Test
        @DisplayName("TextChunker(int, int)")
        void textChunker_twoArgConstructor() {
            assertPublicConstructor(TextChunker.class, int.class, int.class);
        }

        @Test
        @DisplayName("DefaultRagPipeline 六参构造器描述符不变")
        void defaultRagPipeline_sixArgConstructor() {
            // 顺序即描述符: 引擎 / 重排 / 上下文拼接 / 提示词 / 生成 / 配置
            assertPublicConstructor(DefaultRagPipeline.class,
                    HybridRetrievalEngine.class,
                    Reranker.class,
                    ContextAssembler.class,
                    RagPromptRenderer.class,
                    AnswerGenerator.class,
                    RagProperties.class);
        }

        @Test
        @DisplayName("HybridRetrievalEngine(List, FusionStrategy, int, long)")
        void hybridRetrievalEngine_fourArgConstructor() {
            assertPublicConstructor(HybridRetrievalEngine.class,
                    List.class, FusionStrategy.class, int.class, long.class);
        }

        @Test
        @DisplayName("ContextAssembler(int, String)")
        void contextAssembler_twoArgConstructor() {
            assertPublicConstructor(ContextAssembler.class, int.class, String.class);
            assertPublicMethod(ContextAssembler.class, "assemble", String.class, List.class);
        }
    }

    // ------------------------------------------------------------------
    // 第 7 类：配置类与嵌套结构（SIA 用 Binder 绑 nebula.ai.rag 前缀）
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("7. 配置类: RagProperties 前缀与嵌套结构")
    class ConfigurationSurface {

        @Test
        @DisplayName("前缀仍是 nebula.ai.rag(SIA 生产 yml 与迁移测试都按此绑定)")
        void prefix_isPreserved() {
            ConfigurationProperties annotation =
                    RagProperties.class.getAnnotation(ConfigurationProperties.class);
            assertThat(annotation)
                    .as("RagProperties 必须保留 @ConfigurationProperties")
                    .isNotNull();
            String prefix = annotation.prefix().isEmpty() ? annotation.value() : annotation.prefix();
            assertThat(prefix).isEqualTo("nebula.ai.rag");
        }

        @Test
        @DisplayName("七个嵌套结构与顶层访问器齐全")
        void nestedGroups_areComplete() {
            assertPublicMethod(RagProperties.class, "isEnabled", boolean.class);
            assertNestedGroup("Retrieval", "getRetrieval");
            assertNestedGroup("Fusion", "getFusion");
            assertNestedGroup("Rerank", "getRerank");
            assertNestedGroup("Context", "getContext");
            assertNestedGroup("Generation", "getGeneration");
            assertNestedGroup("Chunking", "getChunking");
            assertNestedGroup("Degrade", "getDegrade");
        }

        @Test
        @DisplayName("SIA RagConfigMigrationTest 逐项读到的叶子访问器齐全")
        void leafAccessors_areComplete() {
            assertPublicMethod(nested("Retrieval"), "getTopK", int.class);
            assertPublicMethod(nested("Retrieval"), "getCandidateMultiplier", int.class);
            assertPublicMethod(nested("Retrieval"), "getTimeoutSeconds", int.class);
            assertPublicMethod(nested("Fusion"), "getRrfK", int.class);
            assertPublicMethod(nested("Fusion"), "getSourcePriority", List.class);
            assertPublicMethod(nested("Rerank"), "isEnabled", boolean.class);
            assertPublicMethod(nested("Rerank"), "getTopK", int.class);
            assertPublicMethod(nested("Rerank"), "getTimeoutMs", int.class);
            assertPublicMethod(nested("Context"), "getMaxLength", int.class);
            assertPublicMethod(nested("Context"), "getDocumentTemplate", String.class);
            assertPublicMethod(nested("Generation"), "getTimeoutMs", long.class);
            assertPublicMethod(nested("Chunking"), "getSize", int.class);
            assertPublicMethod(nested("Chunking"), "getOverlap", int.class);
            assertPublicMethod(nested("Degrade"), "getNoDocumentAnswer", String.class);
            assertPublicMethod(nested("Degrade"), "getFallbackHeader", String.class);
            assertPublicMethod(nested("Degrade"), "getFallbackFooter", String.class);
            assertPublicMethod(nested("Degrade"), "getFallbackExcerptLength", int.class);
        }

        private void assertNestedGroup(String simpleName, String accessor) {
            Class<?> nested = nested(simpleName);
            assertPublicMethod(RagProperties.class, accessor, nested);
            assertPublicMethod(RagProperties.class,
                    "set" + simpleName, void.class, nested);
        }

        private Class<?> nested(String simpleName) {
            for (Class<?> candidate : RagProperties.class.getDeclaredClasses()) {
                if (candidate.getSimpleName().equals(simpleName)) {
                    assertThat(Modifier.isPublic(candidate.getModifiers()))
                            .as("RagProperties." + simpleName + " 必须是 public 嵌套类")
                            .isTrue();
                    return candidate;
                }
            }
            return fail("RagProperties 缺少嵌套类 " + simpleName + "(详细设计 §1 配置类红线)");
        }
    }

    // ------------------------------------------------------------------
    // 第 8 类：跨红线类型的全局不变式
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("8. 全局不变式: 红线类型 public 且可被下游继承/实现")
    class GlobalInvariants {

        /** §1 表格覆盖到的全部类型 */
        private final List<Class<?>> redLineTypes = List.of(
                Retriever.class, Reranker.class, AnswerGenerator.class, RagPromptRenderer.class,
                RagPipeline.class, FusionStrategy.class,
                VectorStoreRetriever.class, DefaultRagPipeline.class, HybridRetrievalEngine.class,
                RrfFusionStrategy.class, TextChunker.class,
                ContextAssembler.class,
                RetrievalResult.class, RagQuery.class, RagAnswer.class, RagProperties.class);

        @Test
        @DisplayName("全部红线类型 public 且非包私有")
        void allRedLineTypes_arePublic() {
            List<String> violations = new ArrayList<>();
            for (Class<?> type : redLineTypes) {
                if (!Modifier.isPublic(type.getModifiers())) {
                    violations.add(type.getName() + " 不是 public");
                }
            }
            assertThat(violations)
                    .as("红线类型必须保持 public(详细设计 §1)")
                    .isEmpty();
        }

        @Test
        @DisplayName("被继承/实现的类型均非 final")
        void extendableTypes_areNotFinal() {
            List<Class<?>> extendable = List.of(
                    VectorStoreRetriever.class, DefaultRagPipeline.class,
                    RrfFusionStrategy.class);
            List<String> violations = new ArrayList<>();
            for (Class<?> type : extendable) {
                if (Modifier.isFinal(type.getModifiers())) {
                    violations.add(type.getName() + " 变成了 final");
                }
            }
            assertThat(violations)
                    .as("SIA 或框架自动装配依赖这些类型可被继承/代理")
                    .isEmpty();
        }

        @Test
        @DisplayName("红线包名不变(SIA import 逐个写死了全限定名)")
        void packageNames_areStable() {
            assertThat(Retriever.class.getPackageName()).isEqualTo("io.nebula.ai.rag.retriever");
            assertThat(Reranker.class.getPackageName()).isEqualTo("io.nebula.ai.rag.rerank");
            assertThat(RagPipeline.class.getPackageName()).isEqualTo("io.nebula.ai.rag.pipeline");
            assertThat(FusionStrategy.class.getPackageName()).isEqualTo("io.nebula.ai.rag.fusion");
            assertThat(TextChunker.class.getPackageName()).isEqualTo("io.nebula.ai.rag.chunking");
            assertThat(RagProperties.class.getPackageName()).isEqualTo("io.nebula.ai.rag.config");
        }
    }

    // ------------------------------------------------------------------
    // 反射断言工具：失败信息一律指向详细设计 §1
    // ------------------------------------------------------------------

    private static final String HINT = "(详细设计 §1 兼容红线; 破坏它等于破坏 SIA 生产)";

    private static void assertInterface(Class<?> type) {
        assertThat(type.isInterface())
                .as(type.getName() + " 必须保持接口 " + HINT)
                .isTrue();
        assertThat(Modifier.isPublic(type.getModifiers()))
                .as(type.getName() + " 必须是 public " + HINT)
                .isTrue();
    }

    private static Method assertPublicMethod(Class<?> owner, String name,
                                             Class<?> returnType, Class<?>... paramTypes) {
        Method method;
        try {
            method = owner.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return fail(owner.getName() + " 缺少方法 " + name
                    + Arrays.toString(paramTypes) + " " + HINT);
        }
        assertThat(method.getReturnType())
                .as(owner.getName() + "#" + name + " 返回类型被改动 " + HINT)
                .isEqualTo(returnType);
        assertThat(Modifier.isPublic(method.getModifiers()))
                .as(owner.getName() + "#" + name + " 必须是 public " + HINT)
                .isTrue();
        return method;
    }

    private static Constructor<?> assertPublicConstructor(Class<?> owner, Class<?>... paramTypes) {
        try {
            Constructor<?> ctor = owner.getConstructor(paramTypes);
            assertThat(Modifier.isPublic(ctor.getModifiers()))
                    .as(owner.getName() + " 构造器必须是 public " + HINT)
                    .isTrue();
            return ctor;
        } catch (NoSuchMethodException e) {
            return fail(owner.getName() + " 缺少构造器 " + Arrays.toString(paramTypes) + " " + HINT);
        }
    }

    private static void assertAbstract(Method method) {
        assertThat(Modifier.isAbstract(method.getModifiers()))
                .as(method.getDeclaringClass().getName() + "#" + method.getName()
                        + " 必须保持抽象 " + HINT)
                .isTrue();
    }

    private static void assertOverridable(Method method) {
        assertThat(Modifier.isFinal(method.getModifiers()))
                .as(method.getDeclaringClass().getName() + "#" + method.getName()
                        + " 不能是 final: 下游要覆盖它 " + HINT)
                .isFalse();
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as(method.getDeclaringClass().getName() + "#" + method.getName()
                        + " 不能是 static " + HINT)
                .isFalse();
    }

    private static void assertSingleAbstractMethod(Class<?> type) {
        List<String> abstractMethods = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                abstractMethods.add(method.getName());
            }
        }
        assertThat(abstractMethods)
                .as(type.getName() + " 必须保持单抽象方法: SIA 用 lambda 实现它 " + HINT)
                .hasSize(1);
    }

    private static void assertConstant(Class<?> owner, String name, Class<?> type, Object value) {
        Field field;
        try {
            field = owner.getField(name);
        } catch (NoSuchFieldException e) {
            fail(owner.getName() + " 缺少常量 " + name + " " + HINT);
            return;
        }
        int modifiers = field.getModifiers();
        assertThat(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers))
                .as(owner.getName() + "." + name + " 必须是 public static final " + HINT)
                .isTrue();
        assertThat(field.getType())
                .as(owner.getName() + "." + name + " 类型被改动 " + HINT)
                .isEqualTo(type);
        Object actual;
        try {
            actual = field.get(null);
        } catch (IllegalAccessException e) {
            fail(owner.getName() + "." + name + " 不可读 " + HINT);
            return;
        }
        assertThat(actual)
                .as(owner.getName() + "." + name + " 的值被改动: 该值已被 javac 内联进 SIA 字节码 " + HINT)
                .isEqualTo(value);
    }

    /**
     * 断言模型保留 Lombok builder：静态 builder() + 逐属性方法 + build()
     */
    private static void assertBuilder(Class<?> model, String... properties) {
        Method builderMethod;
        try {
            builderMethod = model.getMethod("builder");
        } catch (NoSuchMethodException e) {
            fail(model.getName() + " 缺少静态 builder() " + HINT);
            return;
        }
        assertThat(Modifier.isStatic(builderMethod.getModifiers()))
                .as(model.getName() + "#builder 必须是静态方法 " + HINT)
                .isTrue();

        Class<?> builderType = builderMethod.getReturnType();
        List<String> missing = new ArrayList<>();
        for (String property : properties) {
            boolean found = false;
            for (Method method : builderType.getMethods()) {
                if (method.getName().equals(property) && method.getParameterCount() == 1
                        && method.getReturnType().equals(builderType)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(property);
            }
        }
        assertThat(missing)
                .as(model.getName() + " 的 builder 缺少属性方法 " + HINT)
                .isEmpty();

        try {
            Method build = builderType.getMethod("build");
            assertThat(build.getReturnType())
                    .as(model.getName() + " 的 builder.build() 返回类型被改动 " + HINT)
                    .isEqualTo(model);
        } catch (NoSuchMethodException e) {
            fail(model.getName() + " 的 builder 缺少 build() " + HINT);
        }
    }
}
