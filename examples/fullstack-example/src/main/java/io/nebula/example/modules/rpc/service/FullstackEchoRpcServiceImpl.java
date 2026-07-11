package io.nebula.example.modules.rpc.service;

import io.nebula.rpc.core.annotation.RpcService;

@RpcService
public class FullstackEchoRpcServiceImpl implements FullstackEchoRpcService {

    @Override
    public String echo(String message) {
        return "fullstack:" + message;
    }
}
