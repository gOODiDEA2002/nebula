#!/usr/bin/env bash
# proxy-pool 启动入口
# 后台跑 API server, 前台跑 scheduler. scheduler 异常退出会触发容器 restart.
set -e
python proxyPool.py server &
exec python proxyPool.py schedule
