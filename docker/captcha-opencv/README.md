# OpenCV 验证码识别服务

基于 Python + OpenCV 的验证码识别服务，提供滑块验证码缺口检测和旋转验证码角度识别功能。

## 功能特性

- **滑块验证码检测**: 识别滑块验证码的缺口位置，返回偏移量
- **旋转验证码检测**: 识别图片需要旋转的角度
- **RESTful API**: 提供简单易用的 HTTP 接口
- **Docker 部署**: 开箱即用的容器化部署

## 快速开始

### 1. 启动服务

```bash
# 进入目录
cd nebula/docker/captcha-opencv

# 设置脚本执行权限
chmod +x start.sh stop.sh

# 启动服务（首次会构建镜像）
./start.sh --build

# 或者直接启动（使用已有镜像）
./start.sh
```

### 2. 验证服务

```bash
# 健康检查
curl http://localhost:8867/ping

# 预期返回
{"service":"captcha-opencv","status":"ok","version":"1.0.0"}
```

### 3. 可视化测试

访问浏览器测试页面，可直观验证服务功能：

http://localhost:8867/test/page

### 4. 停止服务

```bash
./stop.sh
```

## API 接口

### 健康检查

```
GET /ping
```

响应：
```json
{
    "status": "ok",
    "service": "captcha-opencv",
    "version": "1.0.0"
}
```

### 滑块验证码检测

```
POST /slider/detect
Content-Type: application/x-www-form-urlencoded

background=<背景图Base64>
slider=<滑块图Base64>  // 可选
```

响应：
```json
{
    "success": true,
    "offset": 156,      // 缺口偏移量（像素）
    "confidence": 0.92  // 置信度 (0-1)
}
```

### 旋转验证码检测

```
POST /rotate/detect
Content-Type: application/x-www-form-urlencoded

image=<验证码图片Base64>
```

响应：
```json
{
    "success": true,
    "angle": 45,        // 需要旋转的角度
    "confidence": 0.85  // 置信度 (0-1)
}
```

## Java 集成

在 Nebula 框架中配置 OpenCV 服务：

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      local-slider-enabled: true
      opencv-urls: 
        - http://localhost:8867
```

使用示例：

```java
@Autowired
private CaptchaManager captchaManager;

// 识别滑块验证码
CaptchaRequest request = CaptchaRequest.builder()
    .type(CaptchaType.SLIDER)
    .backgroundImage(backgroundBase64)
    .sliderImage(sliderBase64)
    .timeout(30000)
    .build();

CaptchaResult result = captchaManager.solve(request);
if (result.isSuccess()) {
    int offset = result.getSliderOffset();
    // 使用 offset 执行滑块拖动
}
```

## 算法说明

### 滑块缺口检测

1. **模板匹配算法**（有滑块图时）
   - 将背景图和滑块图转为灰度图
   - 使用 Canny 边缘检测提取轮廓
   - 使用 `cv2.matchTemplate` 进行模板匹配
   - 返回最佳匹配位置作为偏移量

2. **轮廓检测算法**（仅有背景图时）
   - 边缘检测 + 形态学闭运算
   - 查找外部轮廓
   - 根据面积、宽高比过滤候选区域
   - 返回最左侧的合理区域

### 旋转角度检测

- 使用霍夫直线变换检测图片中的主要线条
- 统计线条角度的平均值
- 计算使其水平/垂直需要旋转的角度

## 生产部署

### 使用 Harbor 镜像

```yaml
# docker-compose.yml
services:
  captcha-opencv:
    image: harbor.vocoor.com.cn/ci/captcha-opencv:1.0.0
    ports:
      - "8867:8080"
    restart: unless-stopped
```

### 构建并推送镜像

```bash
# 构建镜像
docker build -t harbor.vocoor.com.cn/ci/captcha-opencv:1.0.0 .

# 推送到 Harbor
docker push harbor.vocoor.com.cn/ci/captcha-opencv:1.0.0
```

## 资源需求

- **CPU**: 最低 0.5 核，建议 2 核
- **内存**: 最低 512MB，建议 2GB
- **磁盘**: 约 1GB（主要是 OpenCV 库）

## 常见问题

### 1. 识别准确率不高

- 检查图片是否正确解码
- 尝试调整 Canny 边缘检测参数
- 对于特殊验证码，可能需要定制算法

### 2. 服务启动失败

```bash
# 查看日志
docker-compose logs

# 检查端口是否被占用
lsof -i :8867
```

### 3. 网络连接问题

确保 `crawler-network` 网络存在：

```bash
docker network create crawler-network
```

## 更新日志

### v1.0.0 (2026-01-14)
- 初始版本
- 支持滑块验证码缺口检测
- 支持旋转验证码角度识别
- 提供 Docker 部署方案
