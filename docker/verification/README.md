# 隔离验证环境

该目录为示例应用完整验证提供临时 MySQL 8.3 和 Elasticsearch 9.4.2。两个服务仅监听本机，
使用独立的 Docker Compose 项目和数据卷，不读取或修改 `nebula-data` 的已有容器与数据。

| 服务 | 默认宿主机端口 | 用途 |
| --- | ---: | --- |
| MySQL 8.3 | 13306 | 初始化 Fullstack、分片和 OAuth 示例库 |
| Elasticsearch 9.4.2 | 19200 | 与框架 Java Client 9.4.2 执行真实索引读写 |

推荐通过 E2E 脚本管理生命周期：

```bash
# 单独执行全部中间件协议预检，结束后删除隔离容器和卷
E2E_MODE=full examples/e2e-middleware.sh

# 完整示例验证会自动执行预检，并在所有示例结束后统一清理
E2E_MODE=full examples/e2e-all.sh
```

隔离 MySQL 仅绑定 `127.0.0.1`，使用空密码，不能用于共享开发环境或生产环境。Redis、RabbitMQ、
Nacos、MinIO 和 Chroma 使用现有本地开发服务，但只创建临时资源，并在断言完成后验证资源已删除。
