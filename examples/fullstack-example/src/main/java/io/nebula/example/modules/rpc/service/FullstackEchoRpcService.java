package io.nebula.example.modules.rpc.service;

import io.nebula.rpc.core.annotation.RemoteService;

/**
 * Fullstack 示例对外提供的最小 RPC 服务。
 */
@RemoteService
public interface FullstackEchoRpcService {

    String echo(String message);
}
