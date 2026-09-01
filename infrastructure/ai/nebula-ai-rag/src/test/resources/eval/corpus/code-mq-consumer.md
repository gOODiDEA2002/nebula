# 消息消费者示例

消费者要处理三件事：反序列化失败、业务处理失败、重复投递。下面这段把三件事都写全了。

```java
@Component
public class OrderMessageConsumer {

    private final OrderService orderService;
    private final IdempotentRepository idempotent;

    // 反序列化失败直接进死信，重试多少次都不会突然变得能解析
    @MessageListener(queue = "order-change", deadLetter = "order-change-dlq")
    public void consume(String rawMessage) {
        OrderEvent event;
        try {
            event = deserialize(rawMessage);
        } catch (Exception e) {
            throw new NonRetryableException("消息体无法解析");
        }

        // 幂等判断放在业务处理之前：重复投递是常态而不是异常情况
        if (idempotent.processed(event.getMessageId())) {
            return;
        }

        try {
            orderService.handle(event);
            idempotent.markProcessed(event.getMessageId());
        } catch (RetryableException e) {
            // 可重试异常抛回队列，重投最大次数由消息模块统一控制
            throw e;
        } catch (Exception e) {
            // 其余异常一律进死信，避免坏消息在队列里无限循环消费
            throw new NonRetryableException("业务处理失败");
        }
    }
}
```

幂等标记的过期时间要长于消息的最大重投周期，否则过期之后同一条消息会被再处理一次。

## 相关模块速览

- 模块端口表里的默认超时列给的是各模块单路调用的超时上限，端口一旦分配就不再回收。
- 配置键表里的默认值与是否必填两列最容易被混淆，不启用对应能力时整行都不生效。
- 错误码表里的是否可重试一列决定调用方该退避重试还是直接返回，错误码分配后不再复用。
- 示例检索器在构造期校验权重不能为负数，检索期把单路失败收敛成空表，并用超时方法给出本路自己的毫秒数。
- 管线装配代码里检索器列表为空要当成配置事故抛异常，候选放大倍数与上下文预算都写死在装配处，构造器参数一个都不能少。
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
