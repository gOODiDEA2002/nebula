# ddddocr 验证码识别服务 Dockerfile
# 提供通用验证码识别功能
#
# 版本说明：
# - 基于 Python 3.11-slim
# - 提供 REST API 接口

FROM python:3.11-slim

# 设置标签
LABEL name="captcha-ddddocr" \
      version="1.0.0" \
      project="ci" \
      platform="linux/amd64,linux/arm64" \
      maintainer="andy" \
      description="ddddocr Captcha Recognition Service"

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖 (ddddocr 可能需要一些基础库)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制依赖文件
COPY requirements.txt .

# 安装 Python 依赖
# 注意：ddddocr 首次运行可能需要下载模型，我们在构建阶段尝试预加载或让其在启动时下载
# 为了加速构建，可以使用国内源
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 复制应用代码
COPY app.py .

# 暴露端口
EXPOSE 8866

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
    CMD curl -sf http://localhost:8866/ping || exit 1

# 启动服务
# 使用 Gunicorn 进行生产环境部署，设置 2 个 worker (根据CPU核心数调整)
CMD ["gunicorn", "--workers=2", "--threads=4", "--bind=0.0.0.0:8866", "app:app"]
