# ddddocr 验证码识别服务

基于 Python + ddddocr 的通用验证码识别服务。

## 功能特性

- **通用验证码识别**: 识别数字、字母、汉字等通用验证码
- **RESTful API**: 提供简单易用的 HTTP 接口
- **Docker 部署**: 开箱即用的容器化部署
- **负载均衡**: 支持多实例部署，提高并发处理能力

## 快速开始

### 1. 启动服务

```bash
# 进入目录
cd nebula/docker/captcha-ddddocr

# 设置脚本执行权限
chmod +x start.sh stop.sh

# 启动服务
./start.sh
```

### 2. 验证服务

```bash
# 健康检查
curl http://localhost:8866/ping

# 预期返回
{"service":"captcha-ddddocr","status":"ok","version":"1.0.0"}
```

### 3. 可视化测试

访问浏览器测试页面，可直观验证服务功能：

http://localhost:8866/test/page

### 4. 停止服务

```bash
./stop.sh
```

## API 接口

### 健康检查

```
GET /ping
```

### Base64 图片识别

```
POST /ocr/b64/text
Content-Type: application/json

{
    "image": "<Base64编码的图片字符串>"
}
```

响应：
```json
{
    "success": true,
    "result": "识别结果"
}
```

## Java 集成

在 Nebula 框架中配置 ddddocr 服务：

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      ocr-engine: ddddocr
      ddddocr-urls: 
        - http://localhost:8866
        - http://other-host:8866
```

## 资源需求

- **CPU**: 建议 1 核以上
- **内存**: 建议 512MB 以上
