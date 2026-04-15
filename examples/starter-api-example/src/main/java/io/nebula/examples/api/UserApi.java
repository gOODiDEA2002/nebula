package io.nebula.examples.api;

import io.nebula.rpc.core.annotation.RemoteService;

@RemoteService(name = "example-service")
public interface UserApi {
    String hello(String name);
}
