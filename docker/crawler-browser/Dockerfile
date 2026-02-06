# Playwright 浏览器服务 Dockerfile
# 提供远程浏览器服务，支持 Playwright 客户端通过 WebSocket 连接

FROM node:20-slim

LABEL name="browser-playwright" \
      version="1.0.0" \
      project="ci" \
      platform="linux/amd64,linux/arm64" \
      maintainer="andy" \
      description="Playwright Browser Server for Remote Automation"

# 安装 Chromium 运行时依赖
RUN apt-get update && apt-get install -y \
    curl \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libatspi2.0-0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 安装指定版本的 Playwright（必须与客户端匹配）
RUN npm init -y && \
    npm install playwright@1.41.0 && \
    npx playwright install chromium && \
    npx playwright install-deps chromium

# 暴露端口
EXPOSE 9222

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
    CMD curl -sf http://localhost:9222/ || exit 1

# 启动 Playwright Server（支持远程连接）
CMD ["npx", "playwright", "run-server", "--port", "9222", "--host", "0.0.0.0"]
