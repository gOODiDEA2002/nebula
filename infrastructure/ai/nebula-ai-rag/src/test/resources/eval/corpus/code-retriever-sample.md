# 自定义检索器示例

下面这段是接入一路自定义检索的完整写法。三个要点：构造期校验参数、检索期吞掉异常、超时值自己给。示例可以整段拷走再改。

```java
public class SampleRetriever implements Retriever {

    private final SampleClient client;
    private final double weight;

    public SampleRetriever(SampleClient client, double weight) {
        // 构造期校验：权重不能为负数，负权重会让融合公式给出反向排序
        if (weight < 0) {
            throw new IllegalArgumentException("权重不能为负数");
        }
        // 构造期校验：客户端必须就位，缺失时直接拒绝启动，而不是留到运行期空指针
        if (client == null) {
            throw new IllegalArgumentException("检索客户端不能为空");
        }
        this.client = client;
        this.weight = weight;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
        try {
            return client.query(query, topK, filter);
        } catch (Exception e) {
            // 单路失败收敛成空表，绝不向上抛：一路抖动不该拖垮整条链路
            return List.of();
        }
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public long timeoutMillis() {
        // 本路后端是慢查询大户，超时方法返回的毫秒数比全局默认值大一档
        return 2500;
    }
}
```

拷走之后记得把检索器注册进容器，否则混合引擎不会发现它。

## 相关模块速览

- 模块端口表里的默认超时列给的是各模块单路调用的超时上限，端口一旦分配就不再回收。
- 配置键表里的默认值与是否必填两列最容易被混淆，不启用对应能力时整行都不生效。
- 错误码表里的是否可重试一列决定调用方该退避重试还是直接返回，错误码分配后不再复用。
- 管线装配代码里检索器列表为空要当成配置事故抛异常，候选放大倍数与上下文预算都写死在装配处，构造器参数一个都不能少。
- 消费者代码里反序列化失败直接进死信，幂等判断放在业务处理之前，可重试异常抛回队列由消息模块统一重投。
- 网关限流的令牌桶靠补充速率与桶容量两个参数决定形状，两个参数必须一起调，只改一个通常得不到预期效果。
- 本地缓存的过期策略与最大条目数共同决定内存占用，过期时间必须短于远端那一层，否则会出现旧值窗口。
- 搜索索引的分词器下发时机决定词典更新之后能不能生效，写入端与查询端必须用同一套，不一致会出现写进去却搜不到。
- 对象存储分片上传的阈值与每片大小决定重传代价，分片会话过期之后已传的片会被清理，必须重新发起。
- 应用配置文件把各分组的键放在同一棵树下，键路径决定了它属于哪个分组，叶子键名在不同分组里可能重名。
- 检索器注册表里每一路都带权重、超时毫秒与开关，融合分组另外带融合常数与候选放大倍数。
- 索引记录行里每条都带模块、动作、重建次数与状态，失败的那几条要按备注里的处理建议重跑。

## 相关配置键名

下列叶子键名在多个分组里出现过，只看键名无法判断它属于哪个分组：

- `refill-per-second`
- `capacity`
- `max-entries`
- `expire-seconds`
- `multipart-threshold-mb`
- `part-size-mb`
- `timeout-millis`
- `rrf-k`
- `candidate-multiplier`
- `rebuild-count`
- `status`
