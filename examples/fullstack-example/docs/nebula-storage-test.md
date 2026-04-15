# Nebula 对象存储功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 对象存储层的各种功能，包括文件上传下载列表查询删除预签名URL生成等

## 启动应用

### 1. 启动 MinIO 服务

使用 Docker Compose 启动 MinIO：

```bash
cd nebula-middleware
docker-compose up -d minio
```

验证 MinIO 是否启动成功：

```bash
# 检查容器状态
docker ps | grep minio

# 访问 MinIO Console
# 打开浏览器访问: http://localhost:9001
# 默认账号: minioadmin / minioadmin
```

### 2. 配置应用

在 `application.yml` 中配置 MinIO：

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      default-bucket: nebula-files
      auto-create-default-bucket: true
```

### 3. 启动示例应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## API 接口测试

### 1. 上传文件

#### 1.1 基础文件上传

```bash
curl -X POST http://localhost:8000/storage/upload \
  -F "bucket=nebula-files" \
  -F "category=documents" \
  -F "file=@/path/to/your/file.pdf"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "文件上传成功",
  "data": {
    "key": "documents/2024/10/16/a1b2c3d4e5f6_file.pdf",
    "fileName": "file.pdf",
    "fileSize": 1048576,
    "etag": "9b2e2a18e51f3e3f1e1f3e3f1e1f3e3f",
    "bucket": "nebula-files",
    "uploadTime": "2024-10-16T10:30:00Z"
  },
  "success": true
}
```

#### 1.2 上传图片文件

```bash
curl -X POST http://localhost:8000/storage/upload \
  -F "bucket=nebula-files" \
  -F "category=images" \
  -F "file=@/path/to/image.jpg"
```

#### 1.3 上传到自定义分类

```bash
curl -X POST http://localhost:8000/storage/upload \
  -F "bucket=nebula-files" \
  -F "category=reports/2024" \
  -F "file=@/path/to/report.xlsx"
```

### 2. 下载文件

#### 2.1 直接下载文件

```bash
curl -X GET "http://localhost:8000/storage/download?bucket=nebula-files&key=documents/2024/10/16/a1b2c3d4e5f6_file.pdf" \
  -o downloaded_file.pdf
```

#### 2.2 在浏览器中下载

直接在浏览器访问：
```
http://localhost:8000/storage/download?bucket=nebula-files&key=documents/2024/10/16/a1b2c3d4e5f6_file.pdf
```

### 3. 列出文件

#### 3.1 列出所有文件

```bash
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "maxKeys": 10
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "查询文件列表成功",
  "data": {
    "files": [
      {
        "key": "documents/2024/10/16/a1b2c3d4e5f6_file.pdf",
        "fileName": "file.pdf",
        "fileSize": 1048576,
        "formattedSize": "1.0 MB",
        "contentType": "application/pdf",
        "etag": "9b2e2a18e51f3e3f1e1f3e3f1e1f3e3f",
        "lastModified": "2024-10-16T10:30:00",
        "bucket": "nebula-files",
        "extension": "pdf"
      }
    ],
    "total": 1,
    "hasMore": false,
    "nextMarker": null
  },
  "success": true
}
```

#### 3.2 按前缀过滤

```bash
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "prefix": "documents/",
    "maxKeys": 20
  }'
```

#### 3.3 分页查询

```bash
# 第一页
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "prefix": "images/",
    "maxKeys": 10
  }'

# 第二页（使用第一页返回的 nextMarker）
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "prefix": "images/",
    "maxKeys": 10,
    "marker": "images/2024/10/16/lastkey.jpg"
  }'
```

### 4. 生成预签名URL

#### 4.1 生成1小时有效期URL

```bash
curl -X POST http://localhost:8000/storage/presigned-url \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "key": "documents/2024/10/16/a1b2c3d4e5f6_file.pdf",
    "expirySeconds": 3600
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "生成预签名URL成功",
  "data": {
    "url": "http://localhost:9000/nebula-files/documents/2024/10/16/a1b2c3d4e5f6_file.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
    "expirySeconds": 3600,
    "expiryTime": "2024-10-16T11:30:00Z"
  },
  "success": true
}
```

#### 4.2 生成7天有效期URL

```bash
curl -X POST http://localhost:8000/storage/presigned-url \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "key": "documents/2024/10/16/a1b2c3d4e5f6_file.pdf",
    "expirySeconds": 604800
  }'
```

#### 4.3 使用预签名URL下载

复制响应中的 URL，直接在浏览器中访问或使用 curl：

```bash
curl -o file.pdf "http://localhost:9000/nebula-files/documents/2024/10/16/a1b2c3d4e5f6_file.pdf?X-Amz-Algorithm=..."
```

### 5. 删除文件

#### 5.1 删除单个文件

```bash
curl -X DELETE http://localhost:8000/storage/delete \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "keys": ["documents/2024/10/16/a1b2c3d4e5f6_file.pdf"]
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "文件删除操作完成",
  "data": {
    "deletedCount": 1,
    "failedKeys": null
  },
  "success": true
}
```

#### 5.2 批量删除文件

```bash
curl -X DELETE http://localhost:8000/storage/delete \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "keys": [
      "documents/2024/10/16/file1.pdf",
      "documents/2024/10/16/file2.pdf",
      "images/2024/10/16/image1.jpg"
    ]
  }'
```

#### 5.3 删除不存在的文件

```bash
curl -X DELETE http://localhost:8000/storage/delete \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "keys": ["nonexistent/file.pdf"]
  }'
```

响应包含失败的文件键：
```json
{
  "code": "SUCCESS",
  "message": "文件删除操作完成",
  "data": {
    "deletedCount": 0,
    "failedKeys": ["nonexistent/file.pdf"]
  },
  "success": true
}
```

### 6. Bucket 管理

#### 6.1 检查 Bucket 是否存在

```bash
curl -X GET "http://localhost:8000/storage/bucket/exists?bucket=nebula-files"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "查询成功",
  "data": true,
  "success": true
}
```

#### 6.2 创建 Bucket

```bash
curl -X POST "http://localhost:8000/storage/bucket/create?bucket=my-new-bucket"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "创建存储桶成功",
  "data": null,
  "success": true
}
```

## 功能验证清单

###  基础文件操作
- [x] 上传文件 - 成功上传并返回文件信息
- [x] 下载文件 - 正确下载完整文件
- [x] 删除文件 - 成功删除文件
- [x] 批量删除 - 正确处理多个文件删除

###  文件列表功能
- [x] 基础列表 - 正确返回文件列表
- [x] 前缀过滤 - 支持按路径前缀过滤
- [x] 分页查询 - 支持大量文件的分页
- [x] 文件信息 - 返回完整的文件元数据

###  预签名URL功能
- [x] URL生成 - 成功生成预签名URL
- [x] URL访问 - 可以通过URL访问文件
- [x] 过期控制 - 支持自定义过期时间
- [x] 安全性 - 过期后无法访问

###  Bucket管理
- [x] 检查存在 - 正确检查Bucket是否存在
- [x] 创建Bucket - 成功创建新Bucket
- [x] 自动创建 - 启动时自动创建默认Bucket

###  异常处理
- [x] 文件不存在 - 下载不存在的文件时正确处理
- [x] Bucket不存在 - 操作不存在的Bucket时正确处理
- [x] 参数验证 - 无效参数时返回明确错误信息
- [x] 网络错误 - 网络异常时的优雅处理

## 性能测试

### 1. 并发上传测试

```bash
# 使用 Apache Bench 进行并发测试
ab -n 100 -c 10 -p upload.txt -T multipart/form-data \
  http://localhost:8000/storage/upload
```

### 2. 大文件上传测试

```bash
# 创建100MB测试文件
dd if=/dev/zero of=test_100mb.bin bs=1M count=100

# 上传大文件
time curl -X POST http://localhost:8000/storage/upload \
  -F "bucket=nebula-files" \
  -F "category=large-files" \
  -F "file=@test_100mb.bin"
```

### 3. 批量下载测试

```bash
# 批量下载测试
for i in {1..10}; do
  time curl -X GET "http://localhost:8000/storage/download?bucket=nebula-files&key=test/file$i.pdf" \
    -o "downloaded_$i.pdf"
done
```

### 4. 列表查询性能

```bash
# 测试大量文件列表查询
time curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "maxKeys": 1000
  }'
```

## MinIO Console 验证

### 访问 MinIO Console

1. 打开浏览器访问：http://localhost:9001
2. 登录账号：`minioadmin` / `minioadmin`
3. 在 Console 中可以：
   - 查看所有 Bucket
   - 浏览文件列表
   - 手动上传/下载文件
   - 查看文件元数据
   - 管理访问权限

### 验证文件上传

1. 通过 API 上传文件后
2. 在 MinIO Console 中找到对应的 Bucket
3. 验证文件是否存在
4. 检查文件大小和元数据是否正确

### 验证文件删除

1. 通过 API 删除文件后
2. 在 MinIO Console 中刷新文件列表
3. 确认文件已被删除

## 使用 Swagger UI 测试

### 访问 Swagger UI

启动应用后，访问：http://localhost:8000/swagger-ui.html

### 测试步骤

1. **找到存储API**
   - 在 Swagger UI 中找到 "对象存储演示" 标签
   - 展开查看所有可用接口

2. **测试文件上传**
   - 点击 `/storage/upload` 接口
   - 点击 "Try it out"
   - 填写参数并选择文件
   - 点击 "Execute" 执行

3. **测试其他接口**
   - 使用上传返回的 `key` 测试下载删除等操作
   - 查看响应结果和状态码

## 故障排查

### 1. MinIO 连接失败

**现象**：应用启动报错 "MinIO客户端初始化失败"

**排查步骤**：
```bash
# 检查 MinIO 是否启动
docker ps | grep minio

# 检查端口是否可访问
telnet localhost 9000

# 查看 MinIO 日志
docker logs nebula-minio
```

**解决方案**：
- 确保 MinIO 服务已启动
- 检查配置的 endpoint 是否正确
- 检查防火墙设置

### 2. 文件上传失败

**现象**：上传文件时返回 "上传失败" 错误

**排查步骤**：
```bash
# 检查文件大小限制
# application.yml 中的 max-file-size 配置

# 检查磁盘空间
df -h

# 查看应用日志
tail -f logs/nebula-example.log
```

**解决方案**：
- 检查文件大小是否超过限制
- 确保磁盘空间充足
- 增加超时配置

### 3. 下载文件失败

**现象**：下载文件时报 404 或文件损坏

**排查步骤**：
```bash
# 验证文件是否存在
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{"bucket": "nebula-files"}'

# 检查文件key是否正确
# 注意URL编码问题
```

**解决方案**：
- 确认文件 key 拼写正确
- 注意特殊字符需要URL编码
- 检查文件是否已被删除

### 4. 预签名URL无法访问

**现象**：生成的预签名URL返回 403 Forbidden

**可能原因**：
- URL已过期
- MinIO服务器时间不同步
- AccessKey/SecretKey不匹配

**解决方案**：
```bash
# 检查服务器时间
date

# 重新生成URL
# 使用较长的过期时间进行测试
```

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.storage: DEBUG
    io.nebula.example.modules.storage: DEBUG
    io.minio: DEBUG
```

## 最佳实践验证

### 1. 文件命名规范

验证生成的文件 key 格式：
```
category/YYYY/MM/DD/uuid_originalname.ext
```

示例：
```
documents/2024/10/16/a1b2c3d4e5f6789_report.pdf
images/2024/10/16/b2c3d4e5f6789a1_photo.jpg
```

### 2. 元数据完整性

上传文件后，检查元数据：
```bash
# 列出文件，查看返回的元数据
curl -X POST http://localhost:8000/storage/list \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "prefix": "documents/"
  }'
```

验证以下信息：
- contentType 正确
- fileSize 准确
- lastModified 时间正确
- etag 存在

### 3. 安全性验证

#### 测试文件访问权限
```bash
# 直接访问文件（应该失败，需要预签名URL）
curl http://localhost:9000/nebula-files/documents/file.pdf

# 使用预签名URL访问（应该成功）
curl "http://localhost:9000/nebula-files/documents/file.pdf?X-Amz-..."
```

#### 测试URL过期
```bash
# 生成60秒过期的URL
curl -X POST http://localhost:8000/storage/presigned-url \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "nebula-files",
    "key": "documents/file.pdf",
    "expirySeconds": 60
  }'

# 等待60秒后再访问（应该失败）
sleep 61
curl "复制的URL"
```

## 集成测试脚本

创建一个完整的测试脚本：

```bash
#!/bin/bash

# 测试脚本
BASE_URL="http://localhost:8000"
BUCKET="nebula-files"

echo "=== Nebula Storage 集成测试 ==="

# 1. 检查Bucket
echo "1. 检查Bucket是否存在..."
curl -s "${BASE_URL}/storage/bucket/exists?bucket=${BUCKET}" | jq

# 2. 上传文件
echo "2. 上传测试文件..."
UPLOAD_RESPONSE=$(curl -s -X POST "${BASE_URL}/storage/upload" \
  -F "bucket=${BUCKET}" \
  -F "category=test" \
  -F "file=@test.txt")
echo $UPLOAD_RESPONSE | jq

FILE_KEY=$(echo $UPLOAD_RESPONSE | jq -r '.data.key')
echo "文件Key: $FILE_KEY"

# 3. 列出文件
echo "3. 列出文件..."
curl -s -X POST "${BASE_URL}/storage/list" \
  -H "Content-Type: application/json" \
  -d "{\"bucket\":\"${BUCKET}\",\"prefix\":\"test/\"}" | jq

# 4. 生成预签名URL
echo "4. 生成预签名URL..."
PRESIGNED_RESPONSE=$(curl -s -X POST "${BASE_URL}/storage/presigned-url" \
  -H "Content-Type: application/json" \
  -d "{\"bucket\":\"${BUCKET}\",\"key\":\"${FILE_KEY}\",\"expirySeconds\":3600}")
echo $PRESIGNED_RESPONSE | jq

# 5. 下载文件
echo "5. 下载文件..."
curl -s "${BASE_URL}/storage/download?bucket=${BUCKET}&key=${FILE_KEY}" -o downloaded.txt
echo "文件已下载到 downloaded.txt"

# 6. 删除文件
echo "6. 删除文件..."
curl -s -X DELETE "${BASE_URL}/storage/delete" \
  -H "Content-Type: application/json" \
  -d "{\"bucket\":\"${BUCKET}\",\"keys\":[\"${FILE_KEY}\"]}" | jq

echo "=== 测试完成 ==="
```

使用方法：
```bash
chmod +x test-storage.sh
./test-storage.sh
```

## 总结

本指南涵盖了 Nebula 对象存储模块的所有核心功能测试：

1.  文件上传（单文件批量不同类型）
2.  文件下载（直接下载浏览器下载）
3.  文件列表（基础查询前缀过滤分页）
4.  文件删除（单个批量）
5.  预签名URL（生成访问过期控制）
6.  Bucket管理（检查创建）
7.  性能测试（并发大文件批量）
8.  异常处理（各种错误场景）

更多详细信息，请参考：
- [Nebula Storage MinIO 模块 README](../../nebula/infrastructure/storage/nebula-storage-minio/README.md)
- [Nebula 框架使用指南](../../nebula/docs/Nebula框架使用指南.md)

