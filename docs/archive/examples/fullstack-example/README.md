# Fullstack 历史测试手册归档

本目录保存 Nebula 2.0 期间按功能拆分的 Fullstack 测试手册。它们在 2026-07-11 归档，原因如下：

- 使用旧端口、旧配置键和旧 `docker-compose` 启动方式。
- 部分端点已经删除或更名。
- 依赖固定业务环境，不能保证测试数据和资源清理。
- 与当前统一 E2E 脚本重复，并产生两套互相冲突的验收口径。

这些文件仅用于历史排查，不应作为 Nebula 2.1 的运行说明。当前入口：

- [Fullstack 示例](../../../../examples/fullstack-example/README.md)
- `E2E_MODE=full examples/fullstack-example/e2e-test.sh`
- [完整验证结果](../../../changes/examples-complete-validation/results.md)
