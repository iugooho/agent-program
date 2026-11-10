package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.CaptchaService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.infrastructure.security.RequestIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 人机验证挑战和票据接口。 */
@RestController
@RequestMapping("/api/v1/auth/captcha")
@Tag(name = "人机验证", description = "自托管图形验证码与一次性验证票据")
public class CaptchaController {

    private final CaptchaService captchaService;
    private final AuthProperties authProperties;

    public CaptchaController(CaptchaService captchaService, AuthProperties authProperties) {
        this.captchaService = captchaService;
        this.authProperties = authProperties;
    }

    /**
     * 创建挑战。
     *
     * @param request HTTP 请求
     * @return 验证码图片
     */
    @PostMapping("/challenge")
    @Operation(summary = "创建人机验证挑战")
    public ApiResponse<CaptchaChallengeResponse> challenge(
            @Valid @RequestBody CaptchaChallengeRequest challengeRequest,
            HttpServletRequest request) {
        return ApiResponse.ok(CaptchaChallengeResponse.from(
                captchaService.create(challengeRequest.purpose(), clientKey(request))));
    }

    private String clientKey(HttpServletRequest request) {
        return RequestIdentity.clientKey(
                request,
                authProperties.rateLimit().trustForwardedHeader());
    }
}
