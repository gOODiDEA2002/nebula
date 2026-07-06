# -*- coding: utf-8 -*-
"""
proxy-pool 配置文件 (nebula/docker/proxy-pool 调优版本)

跟上游 jhao104/proxy_pool 相比的关键调整:
  PROXY_FETCHER       12 个 -> 3 个 (精选稳定源)
  VERIFY_TIMEOUT      10s   -> 3s  (失败连接更快释放 fd)
  MAX_FAIL_COUNT      0     -> 3   (避免单次失败立即剔除引发的 churn)
  POOL_SIZE_MIN       20    -> 10
所有值仍可通过环境变量覆盖。
"""

BANNER = r"""
****************************************************************
*** ______  ********************* ______ *********** _  ********
*** | ___ \_ ******************** | ___ \ ********* | | ********
*** | |_/ / \__ __   __  _ __   _ | |_/ /___ * ___  | | ********
*** |  __/|  _// _ \ \ \/ /| | | ||  __// _ \ / _ \ | | ********
*** | |   | | | (_) | >  < \ |_| || |  | (_) | (_) || |___  ****
*** \_|   |_|  \___/ /_/\_\ \__  |\_|   \___/ \___/ \_____/ ****
****                       __ / /                          *****
************************* /___ / *******************************
*************************       ********************************
****************************************************************
"""

VERSION = "2.4.0-nebula"

# ############### server config ###############
HOST = "0.0.0.0"
PORT = 5010

# API 访问 token, 由 docker-compose 通过 API_TOKEN 环境变量注入;
# 留空则不启用鉴权(与上游 jhao104/proxy_pool 一致的开放行为)
API_TOKEN = ""

# ############### database config ###################
# 由 docker-compose 通过 DB_CONN 环境变量注入
DB_CONN = 'redis://:pwd@127.0.0.1:6379/0'

# proxy table name
TABLE_NAME = 'use_proxy'

# anonymous proxy table name
ANONYMOUS_TABLE_NAME = 'anonymous_proxy'

# anonymous check url
ANONYMOUS_CHECK_URL = 'http://httpbin.org/ip'


# ###### config the proxy fetch function ######
# 精选 3 个相对稳定的源 (其余源命中率太低且耗 fd, 已禁用)
# 如需启用更多源, 改这里即可: freeProxy01 ~ freeProxy11, freeProxyExternal
PROXY_FETCHER = [
    "freeProxy01",
    "freeProxy02",
    "freeProxy03",
]

# ############# proxy validator #################
HTTP_URL = "http://httpbin.org"
HTTPS_URL = "https://www.qq.com"

# 单个代理验证超时 (秒). 越短 fd 释放越快, 但可能误杀慢但可用的代理
VERIFY_TIMEOUT = 3

# 近 N 次校验中允许的最大失败次数, 超过则剔除代理 (上游默认 0 太激进)
MAX_FAIL_COUNT = 3

# 池子代理数少于此值时触发抓取
POOL_SIZE_MIN = 10

# ############# proxy attributes #################
PROXY_REGION = True

# ############# scheduler config #################
TIMEZONE = "Asia/Shanghai"
