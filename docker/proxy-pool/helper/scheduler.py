# -*- coding: utf-8 -*-
"""
proxy 调度器 (nebula/docker/proxy-pool 调优版本)

跟上游 jhao104/proxy_pool 相比的关键调整:
  proxy_fetch 间隔   4 min  -> 30 min  (拉新代理频率)
  proxy_check 间隔   2 min  -> 10 min  (验证现有代理频率)
  threadpool         20     -> 5       (并发降低, 不打爆下游)
  max_instances      10     -> 1       (job 同一时刻只允许 1 个实例)
  coalesce           False  -> True    (错过的轮次合并而非累加)
"""
__author__ = 'JHao'

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.executors.pool import ProcessPoolExecutor

from util.six import Queue
from helper.fetch import Fetcher
from helper.check import Checker
from handler.logHandler import LogHandler
from handler.proxyHandler import ProxyHandler
from handler.configHandler import ConfigHandler


def __runProxyFetch():
    proxy_queue = Queue()
    proxy_fetcher = Fetcher()

    for proxy in proxy_fetcher.run():
        proxy_queue.put(proxy)

    Checker("raw", proxy_queue)


def __runProxyCheck():
    proxy_handler = ProxyHandler()
    proxy_queue = Queue()
    if proxy_handler.db.getCount().get("total", 0) < proxy_handler.conf.poolSizeMin:
        __runProxyFetch()
    for proxy in proxy_handler.getAll():
        proxy_queue.put(proxy)
    Checker("use", proxy_queue)

    # 常规检测完成后执行匿名性检测
    __runAnonymousCheck()


def __runAnonymousCheck():
    """匿名性检测 (增强版)"""
    from helper.anonymousChecker import AnonymousChecker
    checker = AnonymousChecker()
    checker.run()


def runScheduler():
    __runProxyFetch()

    timezone = ConfigHandler().timezone
    scheduler_log = LogHandler("scheduler")
    scheduler = BlockingScheduler(logger=scheduler_log, timezone=timezone)

    # 调优: 频率拉长, 减轻下游 / fd 压力
    scheduler.add_job(__runProxyFetch, 'interval', minutes=30, id="proxy_fetch", name="proxy采集")
    scheduler.add_job(__runProxyCheck, 'interval', minutes=10, id="proxy_check", name="proxy检查")

    executors = {
        'default': {'type': 'threadpool', 'max_workers': 5},
        'processpool': ProcessPoolExecutor(max_workers=2)
    }
    job_defaults = {
        'coalesce': True,
        'max_instances': 1,
    }

    scheduler.configure(executors=executors, job_defaults=job_defaults, timezone=timezone)

    scheduler.start()


if __name__ == '__main__':
    runScheduler()
