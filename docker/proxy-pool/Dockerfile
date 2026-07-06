# proxy-pool 公共代理池 Dockerfile
# 基于 jhao104/proxy_pool 二次定制
#
# 版本说明:
# - 基于 Python 3.11-slim (原版 3.6-alpine 已 EOL)
# - 内置 PROXY_FETCHER / VERIFY_TIMEOUT / scheduler 间隔等调优默认值
# - 提供 HEALTHCHECK 与优雅停机

FROM python:3.11-slim

LABEL name="proxy-pool" \
      version="1.1.0" \
      project="ci" \
      platform="linux/amd64,linux/arm64" \
      maintainer="andy" \
      description="Public Proxy Pool Service (jhao104/proxy_pool customized)"

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TZ=Asia/Shanghai

# 系统依赖: lxml 需要 libxml2/libxslt, curl 给 HEALTHCHECK 用
RUN apt-get update && apt-get install -y --no-install-recommends \
        libxml2 \
        libxslt1.1 \
        curl \
        tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

# 构建期临时依赖装完即删
RUN apt-get update && apt-get install -y --no-install-recommends gcc libxml2-dev libxslt1-dev \
    && pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple \
    && apt-get purge -y gcc libxml2-dev libxslt1-dev \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

COPY . .

EXPOSE 5010

# 优雅停机
STOPSIGNAL SIGTERM

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -sf http://localhost:5010/ || exit 1

ENTRYPOINT ["sh", "start.sh"]
