# 管线装配示例

管线由六个部件拼成，装配顺序不能乱：引擎在前，重排在中，生成在后。下面是一段可以直接放进配置类的装配代码。

```java
@Configuration
public class QaWiring {

    @Bean
    public HybridEngine hybridEngine(List<Retriever> retrievers, FusionStrategy fusion) {
        // 装配期校验：一个检索器都没有属于配置事故，而不是一种可用状态
        if (retrievers.isEmpty()) {
            throw new IllegalStateException("至少需要一个检索器");
        }
        // 候选放大倍数固定为二：不放大的话第 topK 加一名永远进不了融合环节
        return new HybridEngine(retrievers, fusion, 2, 15000);
    }

    @Bean
    public ContextAssembler contextAssembler() {
        // 上下文预算是整篇跳过而不是截断到一半，半截文档更容易让模型生成错误结论
        return new ContextAssembler(4000, "[文档%d] %s");
    }

    @Bean
    public QaPipeline qaPipeline(HybridEngine engine, Reranker reranker,
                                 ContextAssembler assembler, PromptRenderer renderer,
                                 AnswerGenerator generator, QaProperties properties) {
        // 装配顺序就是构造器参数顺序，六个参数一个都不能少也不能换位置
        return new DefaultQaPipeline(engine, reranker, assembler, renderer, generator, properties);
    }
}
```

这段代码里唯一容易踩的坑是重排器：容器里如果已经有业务自己的实现，框架的空实现会自动退让。

## 相关模块速览

- 模块端口表里的默认超时列给的是各模块单路调用的超时上限，端口一旦分配就不再回收。
- 配置键表里的默认值与是否必填两列最容易被混淆，不启用对应能力时整行都不生效。
- 错误码表里的是否可重试一列决定调用方该退避重试还是直接返回，错误码分配后不再复用。
- 示例检索器在构造期校验权重不能为负数，检索期把单路失败收敛成空表，并用超时方法给出本路自己的毫秒数。
- 消费者代码里反序列化失败直接进死信，幂等判断放在业务处理之前，可重试异常抛回队列由消息模块统一重投。
- 网关限流的令牌桶靠补充速率与桶容量两个参数决定形状，两个参数必须一起调，只改一个通常得不到预期效果。
- 本地缓存的过期策略与最大条目数共同决定内存占用，过期时间必须短于远端那一层，否则会出现旧值窗口。
- 搜索索引的分词器下发时机决定词典更新之后能不能生效，写入端与查询端必须用同一套，不一致会出现写进去却搜不到。
- 对象存储分片上传的阈值与每片大小决定重传代价，分片会话过期之后已传的片会被清理，必须重新发起。
- 应用配置文件把各分组的键放在同一棵树下，键路径决定了它属于哪个分组，叶子键名在不同分组里可能重名。
- 检索器注册表里每一路都带权重、超时毫秒与开关，融合分组另外带融合常数与候选放大倍数。
- 索引记录行里每条都带模块、动作、重建次数与状态，失败的那几条要按备注里的处理建议重跑。

## 相关配置键路径

下列是各分组的父键路径，具体叶子键与取值见对应分组的配置文件：

- `nebula.gateway.rate-limit`
- `nebula.cache.local`
- `nebula.storage.object`
- `nebula.search.index`
- `nebula.messaging.consumer`
