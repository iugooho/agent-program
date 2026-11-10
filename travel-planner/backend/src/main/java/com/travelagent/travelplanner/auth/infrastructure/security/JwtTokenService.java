package com.travelagent.travelplanner.auth.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.application.AccessTokenClaims;
import com.travelagent.travelplanner.auth.application.AuthToken;
import com.travelagent.travelplanner.auth.application.RefreshTokenClaims;
import com.travelagent.travelplanner.auth.application.TokenService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** 基于 HMAC-SHA256 的无状态 JWT 服务。 */
@Service
public class JwtTokenService implements TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final int MIN_SECRET_BYTES = 32;

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final AuthProperties.Jwt properties;

    /**
     * 创建 JWT 服务。
     *
     * @param objectMapper JSON 序列化工具
     * @param authProperties 认证配置
     */
    public JwtTokenService(ObjectMapper objectMapper, AuthProperties authProperties) {
        this.objectMapper = objectMapper;
        this.properties = authProperties.jwt();
        if (properties == null || properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("app.auth.jwt.secret 必须配置");
        }
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.auth.jwt.secret 至少需要 32 字节");
        }
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("app.auth.jwt.issuer 必须配置");
        }
        if (properties.accessTtl() == null || properties.refreshTtl() == null) {
            throw new IllegalStateException("JWT 有效期必须配置");
        }
    }

    @Override
    public AuthToken issue(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plus(properties.accessTtl());
        Instant refreshExpiresAt = issuedAt.plus(properties.refreshTtl());
        String sessionId = UUID.randomUUID().toString();

        Map<String, Object> accessClaims = baseClaims(user, sessionId, TOKEN_TYPE_ACCESS, issuedAt, accessExpiresAt);
        Map<String, Object> refreshClaims = baseClaims(user, sessionId, TOKEN_TYPE_REFRESH, issuedAt, refreshExpiresAt);
        String accessToken = createToken(accessClaims);
        String refreshToken = createToken(refreshClaims);
        return new AuthToken(accessToken, refreshToken, "Bearer", properties.accessTtl().toSeconds());
    }

    @Override
    public AccessTokenClaims validateAccessToken(String token) {
        JsonNode claims = parseAndVerify(token, TOKEN_TYPE_ACCESS, ErrorCode.UNAUTHENTICATED);
        return new AccessTokenClaims(
                longClaim(claims, "sub", ErrorCode.UNAUTHENTICATED),
                textClaim(claims, "username", ErrorCode.UNAUTHENTICATED),
                textClaim(claims, "role", ErrorCode.UNAUTHENTICATED),
                textClaim(claims, "sid", ErrorCode.UNAUTHENTICATED),
                textClaim(claims, "jti", ErrorCode.UNAUTHENTICATED),
                Instant.ofEpochSecond(longClaim(claims, "exp", ErrorCode.UNAUTHENTICATED)));
    }

    @Override
    public RefreshTokenClaims validateRefreshToken(String token) {
        JsonNode claims = parseAndVerify(token, TOKEN_TYPE_REFRESH, ErrorCode.REFRESH_TOKEN_INVALID);
        return new RefreshTokenClaims(
                longClaim(claims, "sub", ErrorCode.REFRESH_TOKEN_INVALID),
                textClaim(claims, "sid", ErrorCode.REFRESH_TOKEN_INVALID),
                textClaim(claims, "jti", ErrorCode.REFRESH_TOKEN_INVALID),
                Instant.ofEpochSecond(longClaim(claims, "exp", ErrorCode.REFRESH_TOKEN_INVALID)));
    }

    @Override
    public Duration refreshTokenLifetime() {
        return properties.refreshTtl();
    }

    private Map<String, Object> baseClaims(
            UserAccount user,
            String sessionId,
            String tokenType,
            Instant issuedAt,
            Instant expiresAt) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", properties.issuer());
        claims.put("sub", user.id());
        claims.put("username", user.username());
        claims.put("role", user.role());
        claims.put("sid", sessionId);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("type", tokenType);
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        return claims;
    }

    private String createToken(Map<String, Object> claims) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        try {
            String encodedHeader = encodeJson(header);
            String encodedClaims = encodeJson(claims);
            String signingInput = encodedHeader + "." + encodedClaims;
            return signingInput + "." + base64Url(sign(signingInput));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法生成 JWT", exception);
        }
    }

    private JsonNode parseAndVerify(String token, String expectedType, ErrorCode errorCode) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(errorCode);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new BusinessException(errorCode);
        }
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                throw new BusinessException(errorCode);
            }
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new BusinessException(errorCode);
            }
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!properties.issuer().equals(claims.path("iss").asText())
                    || !expectedType.equals(claims.path("type").asText())) {
                throw new BusinessException(errorCode);
            }
            long expiresAt = longClaim(claims, "exp", errorCode);
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new BusinessException(errorCode);
            }
            return claims;
        } catch (IllegalArgumentException | java.io.IOException exception) {
            throw new BusinessException(errorCode);
        }
    }

    private long longClaim(JsonNode claims, String name, ErrorCode errorCode) {
        JsonNode value = claims.get(name);
        if (value == null || !value.canConvertToLong()) {
            throw new BusinessException(errorCode);
        }
        return value.longValue();
    }

    private String textClaim(JsonNode claims, String name, ErrorCode errorCode) {
        JsonNode value = claims.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BusinessException(errorCode);
        }
        return value.asText();
    }

    private String encodeJson(Object value) throws JsonProcessingException {
        return base64Url(objectMapper.writeValueAsBytes(value));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("无法签发 JWT", exception);
        }
    }
}
