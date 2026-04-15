package io.nebula.examples.oauth.controller;

import io.nebula.core.common.result.Result;
import io.nebula.examples.oauth.config.OAuthClientConfig;
import io.nebula.examples.oauth.entity.dos.LocalUser;
import io.nebula.examples.oauth.entity.vo.AuthResultVo;
import io.nebula.examples.oauth.entity.vo.CurrentUserVo;
import io.nebula.examples.oauth.service.UserBindingService;
import io.nebula.examples.oauth.service.VocoorOAuthService;
import io.nebula.examples.oauth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import io.nebula.examples.oauth.entity.dto.AuthorizeDto;
/**
 * OAuth 客户端控制器
 * 处理 Vocoor OAuth 登录流程
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth 登录接口")
public class OAuthController {

    private final VocoorOAuthService vocoorOAuthService;
    private final UserBindingService userBindingService;
    private final OAuthClientConfig config;
    private final JwtUtil jwtUtil;

    /**
     * 发起 Vocoor 授权登录
     * 返回授权 URL，前端跳转到该 URL
     */
    @GetMapping("/authorize")
    @Operation(summary = "获取 Vocoor 授权 URL")
    public Result<AuthorizeDto.Response> authorize( AuthorizeDto.Request request) {

        String authUrl = vocoorOAuthService.generateAuthUrl(request.getScope());
        AuthorizeDto.Response response = new AuthorizeDto.Response();
        response.setAuthUrl(authUrl);
        return Result.success(response);
    }

    /**
     * 发起授权并直接重定向
     * 用于页面直接跳转场景
     */
    @GetMapping("/login")
    @Operation(summary = "发起 Vocoor 登录（重定向）")
    public void login(
            @Parameter(description = "权限范围") @RequestParam(required = false) String scope,
            HttpServletResponse response) throws IOException {

        String authUrl = vocoorOAuthService.generateAuthUrl(scope);
        response.sendRedirect(authUrl);
    }

    /**
     * Vocoor 授权回调
     * 处理授权码，完成用户登录
     */
    @GetMapping("/callback")
    @Operation(summary = "Vocoor 授权回调")
    public void callback(
            @Parameter(description = "授权码") @RequestParam String code,
            @Parameter(description = "状态参数") @RequestParam String state,
            @Parameter(description = "错误码") @RequestParam(required = false) String error,
            @Parameter(description = "错误描述") @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletResponse response) throws IOException {

        // 处理授权失败
        if (StringUtils.hasText(error)) {
            log.warn("Vocoor 授权失败: {}, {}", error, errorDescription);
            String redirectUrl = String.format("%s/oauth/result?success=false&error=%s",
                    config.getFrontendUrl(),
                    URLEncoder.encode(errorDescription != null ? errorDescription : error, StandardCharsets.UTF_8));
            response.sendRedirect(redirectUrl);
            return;
        }

        // 处理授权成功
        AuthResultVo result = vocoorOAuthService.handleCallback(code, state);

        if (Boolean.TRUE.equals(result.getSuccess())) {
            // 授权成功，重定向到前端，携带 token
            String redirectUrl = String.format("%s/oauth/result?success=true&token=%s&userId=%d&nickname=%s",
                    config.getFrontendUrl(),
                    URLEncoder.encode(result.getAccessToken(), StandardCharsets.UTF_8),
                    result.getUserId(),
                    URLEncoder.encode(result.getNickname() != null ? result.getNickname() : "", StandardCharsets.UTF_8));
            response.sendRedirect(redirectUrl);
        } else {
            // 授权处理失败
            String redirectUrl = String.format("%s/oauth/result?success=false&error=%s",
                    config.getFrontendUrl(),
                    URLEncoder.encode(result.getErrorMessage() != null ? result.getErrorMessage() : "授权失败", StandardCharsets.UTF_8));
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/current")
    @Operation(summary = "获取当前用户信息")
    public Result<CurrentUserVo> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        // 解析 Token
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Result.error("401", "未登录");
        }

        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return Result.error("401", "Token 无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        LocalUser user = userBindingService.getUserById(userId);

        if (user == null) {
            return Result.error("404", "用户不存在");
        }

        CurrentUserVo vo = CurrentUserVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .build();

        return Result.success(vo);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        // 客户端删除 token 即可
        // 如果需要服务端维护会话，可以在这里实现 token 黑名单
        return Result.success();
    }

}

