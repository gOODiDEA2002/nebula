package io.nebula.example.user.service.rpc;

import io.nebula.example.user.api.dto.AuthDto;
import io.nebula.example.user.api.rpc.AuthRpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.nebula.rpc.core.annotation.RpcService;

@Slf4j
@RpcService
@RequiredArgsConstructor
public class AuthRpcClientImpl implements AuthRpcClient {

  @Override
  public AuthDto.Response auth(AuthDto.Request request) {
    String username = request.getUsername();
    String password = request.getPassword();
    if (username == null || password == null) {
      throw new RuntimeException("用户名或密码不能为空");
    }
    //
    AuthDto.Response response = new AuthDto.Response();
    response.setToken(username + "-" + password); 
    return response;
  }
}
