# Nebula 故障码对照表

以下为示例演示用的虚构故障码，仅用于说明关键词检索路对专有代号的召回价值，非真实框架错误码。

## 向量与检索类

| 故障码 | 含义 | 处置建议 |
|---|---|---|
| NBX-2077 | 向量库连接超时 | 检查 Chroma / Qdrant 主机与端口，确认 `nebula.ai.vector-store` 配置可达 |
| NBX-2081 | embedding 维度不匹配 | 更换 embedding 模型后未重灌，清空集合重新灌库 |
| NBX-2090 | 融合结果为空 | 确认至少一路检索器命中，检查 top-k 与权重设置 |

## 索引治理类

| 故障码 | 含义 | 处置建议 |
|---|---|---|
| NBX-3101 | 索引状态库缺失 | `indexing.enabled=true` 时必须提供 `IndexStateRepository` 实现 |
| NBX-3115 | 写目标部分失败 | 查看 `IndexRunReport.failures`，按块 ID 重入对齐 |

## 生成类

| 故障码 | 含义 | 处置建议 |
|---|---|---|
| NBX-4200 | 生成超时降级 | `RagAnswer.degraded=true`，返回检索片段兜底，调大 `generation.timeout-ms` |
| NBX-4210 | 模型配额耗尽 | 提示 429，稍后重试或更换密钥 |

故障码 `NBX-2077` 是本示例专门设计的生僻代号：向量检索按语义相似大概率漏召，而关键词 2-gram 路必中，用于证明多路融合的互补价值。
