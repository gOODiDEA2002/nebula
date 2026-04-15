package io.nebula.examples.oauth.service.impl;

import io.nebula.examples.oauth.config.OAuthClientConfig;
import io.nebula.examples.oauth.entity.dos.LocalUser;
import io.nebula.examples.oauth.entity.dto.VocoorOAuthDto;
import io.nebula.examples.oauth.entity.vo.AuthResultVo;
import io.nebula.examples.oauth.service.UserBindingService;
import io.nebula.examples.oauth.service.VocoorOAuthService;
import io.nebula.examples.oauth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vocoor OAuth 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VocoorOAuthServiceImpl implements VocoorOAuthService {

    private final OAuthClientConfig config;
    private final WebClient vocoorWebClient;
    private final UserBindingService userBindingService;
    private final JwtUtil jwtUtil;

    /**
     * 存储 state 参数，用于 CSRF 验证
     * 生产环境应使用 Redis
     */
    private final Map<String, Long> stateStore = new ConcurrentHashMap<>();

    @Override
    public String generateAuthUrl(String scope) {
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.put(state, System.currentTimeMillis());

        String actualScope = StringUtils.hasText(scope) ? scope : config.getDefaultScope();

        String authUrl = String.format(
                "%s/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                config.getServerUrl(),
                config.getClientId(),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(actualScope, StandardCharsets.UTF_8),
                state
        );

        log.info("生成授权 URL: {}", authUrl);
        return authUrl;
    }

    @Override
    public AuthResultVo handleCallback(String code, String state) {
        log.info("处理授权回调，code: {}, state: {}", code, state);

        // 1. 验证 state
        Long stateTime = stateStore.remove(state);
        if (stateTime == null) {
            log.warn("State 验证失败: {}", state);
            return AuthResultVo.fail("State 验证失败，可能存在 CSRF 攻击");
        }

        // 检查 state 是否过期（10分钟）
        if (System.currentTimeMillis() - stateTime > 10 * 60 * 1000) {
            log.warn("State 已过期: {}", state);
            return AuthResultVo.fail("授权请求已过期，请重新发起");
        }

        try {
            // 2. 用 code 换取 Token
            VocoorOAuthDto.TokenResult tokenResult = exchangeToken(code);
            if (tokenResult == null || !StringUtils.hasText(tokenResult.getAccessToken())) {
                return AuthResultVo.fail("获取 Token 失败");
            }

            // 3. 获取用户信息
            VocoorOAuthDto.VocoorUser vocoorUser = getUserInfo(tokenResult.getAccessToken());
            if (vocoorUser == null || !StringUtils.hasText(vocoorUser.getOpenid())) {
                return AuthResultVo.fail("获取用户信息失败");
            }

            // 4. 获取或创建本地用户
            LocalUser localUser = userBindingService.getOrCreateLocalUser(vocoorUser);

            // 5. 生成本地 JWT Token
            String localToken = jwtUtil.generateToken(localUser.getId(), localUser.getUsername());

            log.info("用户 {} 授权成功，本地用户ID: {}", vocoorUser.getNickname(), localUser.getId());

            return AuthResultVo.success(
                    localUser.getId(),
                    localUser.getNickname(),
                    localUser.getAvatar(),
                    localToken
            );

        } catch (Exception e) {
            log.error("处理授权回调异常", e);
            return AuthResultVo.fail("授权处理失败: " + e.getMessage());
        }
    }

    @Override
    public VocoorOAuthDto.TokenResult exchangeToken(String code) {
        log.info("使用授权码换取 Token");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", config.getRedirectUri());

        try {
            VocoorOAuthDto.TokenResponse response = vocoorWebClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(VocoorOAuthDto.TokenResponse.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getResult() != null) {
                log.info("Token 获取成功");
                return response.getResult();
            } else {
                log.error("Token 获取失败: {}", response != null ? response.getErrorDescription() : "未知错误");
                return null;
            }
        } catch (Exception e) {
            log.error("Token 获取异常", e);
            return null;
        }
    }

    @Override
    public VocoorOAuthDto.VocoorUser getUserInfo(String accessToken) {
        log.info("获取 Vocoor 用户信息");

        try {
            VocoorOAuthDto.UserInfoResponse response = vocoorWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/oauth/userinfo")
                            .queryParam("client_id", config.getClientId())
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(VocoorOAuthDto.UserInfoResponse.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getResult() != null) {
                log.info("用户信息获取成功: {}", response.getResult().getNickname());
                return response.getResult();
            } else {
                log.error("用户信息获取失败: {}", response != null ? response.getErrorDescription() : "未知错误");
                return null;
            }
        } catch (Exception e) {
            log.error("用户信息获取异常", e);
            return null;
        }
    }

    @Override
    public VocoorOAuthDto.TokenResult refreshToken(String refreshToken) {
        log.info("刷新 Token");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());

        try {
            VocoorOAuthDto.TokenResponse response = vocoorWebClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(VocoorOAuthDto.TokenResponse.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getResult() != null) {
                log.info("Token 刷新成功");
                return response.getResult();
            } else {
                log.error("Token 刷新失败: {}", response != null ? response.getErrorDescription() : "未知错误");
                return null;
            }
        } catch (Exception e) {
            log.error("Token 刷新异常", e);
            return null;
        }
    }
}


