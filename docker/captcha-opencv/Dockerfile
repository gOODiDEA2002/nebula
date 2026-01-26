# OpenCV 验证码识别服务 Dockerfile
# 提供滑块验证码缺口检测、旋转验证码角度识别等功能
#
# 版本说明：
# - 基于 Python 3.11 + OpenCV
# - 提供 REST API 接口

FROM python:3.11-slim

# 设置标签
LABEL name="captcha-opencv" \
      version="2.1.0" \
      project="ci" \
      platform="linux/amd64,linux/arm64" \
      maintainer="andy" \
      description="OpenCV Captcha Recognition Service - With Deep Learning"

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# ========== 基础依赖层 ==========
# 核心依赖很少变化，优先安装以提高缓存命中率
COPY requirements-base.txt .
RUN pip install --no-cache-dir -r requirements-base.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# ========== 应用依赖层 ==========
# 可能新增的依赖，独立一层便于快速更新
COPY requirements-app.txt .
RUN pip install --no-cache-dir -r requirements-app.txt -i https://pypi.tuna.tsinghua.edu.cn/simple || true

# ========== 代码层 ==========
# 代码最后复制，只改代码时构建最快
COPY app.py .
COPY detectors/ ./detectors/
COPY utils/ ./utils/
COPY models/ ./models/

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
    CMD curl -sf http://localhost:8080/ping || exit 1

# 启动服务
# 使用 Gunicorn，2个 workers, 4个 threads
CMD ["gunicorn", "--workers=2", "--threads=4", "--bind=0.0.0.0:8080", "app:app"]
