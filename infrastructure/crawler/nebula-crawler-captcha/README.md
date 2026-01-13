# Nebula Crawler Captcha

验证码识别模块，提供统一的验证码检测、识别和破解能力。

## 功能特性

- **多类型支持**：图形、滑块、点击、手势、旋转、短信、reCAPTCHA、hCaptcha
- **多策略识别**：本地OCR + 第三方打码平台
- **统一接口**：抽象接口，便于扩展
- **自动检测**：自动检测验证码类型
- **优先级调度**：支持多解决器按优先级尝试

## 支持的验证码类型

| 类型 | 枚举值 | 说明 | 本地识别 | 第三方识别 | 成功率参考 |
|------|--------|------|----------|-----------|-----------|
| 图形验证码 | `IMAGE` | 字符识别（数字、字母、算术） | ddddocr | 2Captcha/Anti-Captcha | 95%+ |
| 滑块验证码 | `SLIDER` | 拖动滑块至缺口位置 | OpenCV | 2Captcha | 90%+ |
| 点击验证码 | `CLICK` | 点选特定文字/图片 | OCR+目标检测 | 2Captcha | 85%+ |
| 手势验证码 | `GESTURE` | 九宫格轨迹绘制 | 模式匹配 | 打码平台 | 80%+ |
| 旋转验证码 | `ROTATE` | 旋转图片至正确角度 | OpenCV | 打码平台 | 75%+ |
| 短信验证码 | `SMS` | 手机短信验证 | 不支持 | 接码平台 | 依赖平台 |
| reCAPTCHA | `RECAPTCHA` | Google reCAPTCHA | 不支持 | 2Captcha | 90%+ |
| hCaptcha | `HCAPTCHA` | hCaptcha | 不支持 | 2Captcha | 90%+ |

## 架构设计

```
+-------------------+
|   CaptchaManager  |  <-- 统一入口
+-------------------+
         |
         v
+-------------------+     +------------------+
| CaptchaDetector   | --> | 类型自动检测      |
+-------------------+     +------------------+
         |
         v
+-------------------+
|  CaptchaSolver[]  |  <-- 按优先级排序
+-------------------+
    |    |    |
    v    v    v
 +------+------+------+------+------+
 |Image |Slider|Click |Rotate|Gesture|
 +------+------+------+------+------+
    |         |
    v         v
+--------+ +--------+
|OcrEngine| |OpenCV |
+--------+ +--------+
    |         |
    v         v
+-----------------------+
| CaptchaServiceProvider|
+-----------------------+
    |    |
    v    v
 +--------+----------+
 |2Captcha|Anti-Captcha|
 +--------+----------+
```

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-crawler-captcha</artifactId>
</dependency>
```

### 配置

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      # 本地OCR配置
      local-ocr-enabled: true
      ocr-engine: ddddocr
      ddddocr-url: http://localhost:8866
      # 本地滑块检测（需要OpenCV）
      local-slider-enabled: true
      # 本地旋转检测（需要OpenCV）
      local-rotate-enabled: true
      # 本地点击检测（需要OCR）
      local-click-enabled: true
      # 验证码长度限制
      min-length: 4
      max-length: 6
      # 超时设置
      default-timeout: 60000
      # 第三方打码平台
      providers:
        - name: 2captcha
          api-key: ${CAPTCHA_2CAPTCHA_KEY}
          enabled: true
          priority: 1
        - name: anti-captcha
          api-key: ${CAPTCHA_ANTI_KEY}
          enabled: false
          priority: 2
```

### 使用示例

```java
@Service
@RequiredArgsConstructor
public class CrawlerService {
    
    private final CaptchaManager captchaManager;
    
    public void handleCaptcha() throws CaptchaException {
        // 图形验证码
        CaptchaRequest request = CaptchaRequest.image(imageBase64);
        CaptchaResult result = captchaManager.solve(request);
        if (result.isSuccess()) {
            String text = result.getText();
            // 使用验证码结果
        }
        
        // 滑块验证码
        CaptchaRequest sliderRequest = CaptchaRequest.slider(bgImage, sliderImage);
        CaptchaResult sliderResult = captchaManager.solve(sliderRequest);
        if (sliderResult.isSuccess()) {
            int offset = sliderResult.getSliderOffset();
            // 模拟滑动
        }
        
        // 点击验证码
        CaptchaRequest clickRequest = CaptchaRequest.builder()
            .type(CaptchaType.CLICK)
            .imageBase64(imageBase64)
            .gestureHint("请依次点击：春夏秋冬")
            .build();
        CaptchaResult clickResult = captchaManager.solve(clickRequest);
        if (clickResult.isSuccess()) {
            List<int[]> points = clickResult.getClickPoints();
            // 模拟点击
        }
        
        // 旋转验证码
        CaptchaRequest rotateRequest = CaptchaRequest.builder()
            .type(CaptchaType.ROTATE)
            .imageBase64(imageBase64)
            .build();
        CaptchaResult rotateResult = captchaManager.solve(rotateRequest);
        if (rotateResult.isSuccess()) {
            int angle = rotateResult.getRotateAngle();
            // 旋转图片
        }
        
        // reCAPTCHA
        CaptchaRequest recaptchaRequest = CaptchaRequest.recaptcha(siteUrl, siteKey);
        CaptchaResult recaptchaResult = captchaManager.solve(recaptchaRequest);
        if (recaptchaResult.isSuccess()) {
            String token = recaptchaResult.getRecaptchaToken();
            // 使用token
        }
    }
}
```

---

## 第三方组件部署指南

本模块依赖以下外部组件，请根据实际需求选择部署：

### 1. ddddocr（本地OCR服务）

ddddocr是一款开源的OCR库，专门用于识别各类验证码。

#### 安装要求

- Python 3.7+
- pip

#### Docker 部署（推荐）

```bash
# 拉取镜像
docker pull sml2h3/ddddocr:latest

# 运行容器
docker run -d \
  --name ddddocr \
  -p 8866:8866 \
  --restart=always \
  sml2h3/ddddocr:latest
```

#### 手动部署

```bash
# 创建虚拟环境
python -m venv ddddocr-env
source ddddocr-env/bin/activate  # Linux/Mac
# ddddocr-env\Scripts\activate   # Windows

# 安装依赖
pip install ddddocr flask gunicorn

# 创建服务脚本 ocr_server.py
cat > ocr_server.py << 'EOF'
from flask import Flask, request, jsonify
import ddddocr
import base64

app = Flask(__name__)
ocr = ddddocr.DdddOcr()

@app.route('/ping', methods=['GET'])
def ping():
    return 'pong'

@app.route('/ocr/b64/text', methods=['POST'])
def ocr_b64():
    try:
        image_b64 = request.form.get('image')
        if not image_b64:
            return jsonify({'error': 'No image provided'}), 400
        image_bytes = base64.b64decode(image_b64)
        result = ocr.classification(image_bytes)
        return result
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8866)
EOF

# 启动服务（开发环境）
python ocr_server.py

# 生产环境使用 gunicorn
gunicorn -w 4 -b 0.0.0.0:8866 ocr_server:app
```

#### 验证部署

```bash
# 健康检查
curl http://localhost:8866/ping
# 应返回: pong

# 测试识别（需要提供base64编码的图片）
curl -X POST http://localhost:8866/ocr/b64/text \
  -d "image=<BASE64_IMAGE_DATA>"
```

#### 配置说明

```yaml
nebula:
  crawler:
    captcha:
      local-ocr-enabled: true
      ocr-engine: ddddocr
      ddddocr-url: http://localhost:8866  # ddddocr服务地址
```

---

### 2. OpenCV（本地图像处理）

OpenCV用于滑块验证码的缺口检测和旋转验证码的角度识别。

#### 安装要求

- Java 8+（OpenCV Java绑定）
- 或 Python 3.7+（通过HTTP服务调用）

#### 方案一：OpenCV Python服务

```bash
# 创建虚拟环境
python -m venv opencv-env
source opencv-env/bin/activate

# 安装依赖
pip install opencv-python numpy flask

# 创建服务脚本 opencv_server.py
cat > opencv_server.py << 'EOF'
from flask import Flask, request, jsonify
import cv2
import numpy as np
import base64

app = Flask(__name__)

@app.route('/ping', methods=['GET'])
def ping():
    return 'pong'

@app.route('/slider/detect', methods=['POST'])
def detect_slider_gap():
    """检测滑块缺口位置"""
    try:
        bg_b64 = request.form.get('background')
        slider_b64 = request.form.get('slider')
        
        # 解码图片
        bg_bytes = base64.b64decode(bg_b64)
        slider_bytes = base64.b64decode(slider_b64)
        
        bg_arr = np.frombuffer(bg_bytes, np.uint8)
        slider_arr = np.frombuffer(slider_bytes, np.uint8)
        
        bg_img = cv2.imdecode(bg_arr, cv2.IMREAD_COLOR)
        slider_img = cv2.imdecode(slider_arr, cv2.IMREAD_COLOR)
        
        # 边缘检测
        bg_edge = cv2.Canny(bg_img, 100, 200)
        slider_edge = cv2.Canny(slider_img, 100, 200)
        
        # 模板匹配
        result = cv2.matchTemplate(bg_edge, slider_edge, cv2.TM_CCOEFF_NORMED)
        min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(result)
        
        return jsonify({
            'success': True,
            'offset': max_loc[0],
            'confidence': float(max_val)
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/rotate/detect', methods=['POST'])
def detect_rotate_angle():
    """检测旋转角度"""
    try:
        image_b64 = request.form.get('image')
        
        # 解码图片
        image_bytes = base64.b64decode(image_b64)
        image_arr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(image_arr, cv2.IMREAD_COLOR)
        
        # 转灰度
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # 边缘检测
        edges = cv2.Canny(gray, 50, 150)
        
        # 霍夫变换检测直线
        lines = cv2.HoughLines(edges, 1, np.pi/180, 100)
        
        if lines is not None:
            angles = []
            for rho, theta in lines[:, 0]:
                angle = np.degrees(theta) - 90
                angles.append(angle)
            
            # 取中位数角度
            median_angle = np.median(angles)
            
            return jsonify({
                'success': True,
                'angle': int(round(median_angle)),
                'confidence': 0.7
            })
        
        return jsonify({
            'success': False,
            'error': 'No lines detected'
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8867)
EOF

# 启动服务
python opencv_server.py
```

#### Docker部署

```bash
# Dockerfile
cat > Dockerfile.opencv << 'EOF'
FROM python:3.9-slim

RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install opencv-python-headless numpy flask gunicorn

COPY opencv_server.py /app/

WORKDIR /app
EXPOSE 8867

CMD ["gunicorn", "-w", "2", "-b", "0.0.0.0:8867", "opencv_server:app"]
EOF

# 构建镜像
docker build -t nebula-opencv-service -f Dockerfile.opencv .

# 运行容器
docker run -d \
  --name opencv-service \
  -p 8867:8867 \
  --restart=always \
  nebula-opencv-service
```

#### 配置说明

```yaml
nebula:
  crawler:
    captcha:
      local-slider-enabled: true
      local-rotate-enabled: true
      # OpenCV服务地址（如果使用独立服务）
      opencv-url: http://localhost:8867
```

---

### 3. 2Captcha 打码平台

2Captcha是一家商业打码平台，支持多种验证码类型。

#### 注册账号

1. 访问 [2captcha.com](https://2captcha.com/)
2. 注册账号并充值
3. 在 Dashboard 获取 API Key

#### 支持的验证码类型

| 类型 | 方法 | 说明 |
|------|------|------|
| 图形验证码 | `base64` | 普通字符验证码 |
| reCAPTCHA v2 | `userrecaptcha` | Google reCAPTCHA |
| reCAPTCHA v3 | `userrecaptcha` | 需要传入action参数 |
| hCaptcha | `hcaptcha` | hCaptcha验证码 |
| GeeTest | `geetest` | 极验滑块 |
| FunCaptcha | `funcaptcha` | FunCaptcha |

#### 价格参考（2024）

| 类型 | 价格 |
|------|------|
| 普通图形验证码 | $2.99 / 1000次 |
| reCAPTCHA v2 | $2.99 / 1000次 |
| reCAPTCHA v3 | $2.99 / 1000次 |
| hCaptcha | $2.99 / 1000次 |

#### 配置说明

```yaml
nebula:
  crawler:
    captcha:
      providers:
        - name: 2captcha
          api-key: your-api-key-here
          enabled: true
          priority: 1  # 数字越小优先级越高
```

#### API调用示例

```java
// 框架内部已封装，无需手动调用
// 以下仅供参考

// 提交任务
POST https://2captcha.com/in.php
Content-Type: application/x-www-form-urlencoded

key=YOUR_API_KEY&method=base64&body=BASE64_IMAGE&json=1

// 响应
{"status":1,"request":"TASK_ID"}

// 获取结果（轮询）
GET https://2captcha.com/res.php?key=YOUR_API_KEY&action=get&id=TASK_ID&json=1

// 响应
{"status":1,"request":"识别结果"}
```

---

### 4. Anti-Captcha 打码平台

Anti-Captcha是另一家主流打码平台。

#### 注册账号

1. 访问 [anti-captcha.com](https://anti-captcha.com/)
2. 注册账号并充值
3. 在设置中获取 API Key

#### 配置说明

```yaml
nebula:
  crawler:
    captcha:
      providers:
        - name: anti-captcha
          api-key: your-api-key-here
          enabled: true
          priority: 2
```

---

## Docker Compose 一键部署

以下配置可以一键部署所有本地服务：

```yaml
# docker-compose.captcha.yml
version: '3.8'

services:
  ddddocr:
    image: sml2h3/ddddocr:latest
    container_name: ddddocr
    ports:
      - "8866:8866"
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8866/ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  opencv-service:
    build:
      context: .
      dockerfile: Dockerfile.opencv
    container_name: opencv-service
    ports:
      - "8867:8867"
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8867/ping"]
      interval: 30s
      timeout: 10s
      retries: 3
```

```bash
# 启动服务
docker-compose -f docker-compose.captcha.yml up -d

# 查看状态
docker-compose -f docker-compose.captcha.yml ps

# 查看日志
docker-compose -f docker-compose.captcha.yml logs -f
```

---

## 扩展开发

### 自定义Solver

```java
@Component
public class CustomCaptchaSolver implements CaptchaSolver {
    
    @Override
    public String getName() {
        return "CustomSolver";
    }
    
    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.IMAGE;
    }
    
    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        // 实现识别逻辑
        return CaptchaResult.builder()
            .success(true)
            .type(CaptchaType.IMAGE)
            .text("识别结果")
            .build();
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public int getPriority() {
        return 5; // 自定义优先级
    }
}
```

### 自定义Provider

```java
@Component
public class CustomCaptchaProvider implements CaptchaServiceProvider {
    
    @Override
    public String getName() {
        return "CustomProvider";
    }
    
    @Override
    public boolean supportsType(CaptchaType type) {
        return type == CaptchaType.IMAGE || type == CaptchaType.SLIDER;
    }
    
    @Override
    public CaptchaResult solveImage(byte[] imageData, int timeout) throws CaptchaException {
        // 调用自定义打码平台API
        return CaptchaResult.builder()
            .success(true)
            .type(CaptchaType.IMAGE)
            .text("结果")
            .build();
    }
    
    // 实现其他方法...
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public double getBalance() {
        return 100.0; // 返回余额
    }
}
```

---

## 监控与运维

### 日志配置

```yaml
logging:
  level:
    io.nebula.crawler.captcha: DEBUG
```

### 监控指标

模块会输出以下关键日志：

- 验证码类型自动检测结果
- 解决器选择和尝试过程
- 识别成功率和耗时
- 第三方平台调用情况

### 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| ddddocr连接失败 | 服务未启动 | 检查Docker容器状态 |
| 识别率低 | 验证码类型不支持 | 使用第三方平台 |
| 第三方平台超时 | 网络问题或平台繁忙 | 增加超时时间 |
| 余额不足 | API Key余额为0 | 充值或更换平台 |

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 2.0.1 | 2026-01-08 | 初始版本，支持8种验证码类型 |
