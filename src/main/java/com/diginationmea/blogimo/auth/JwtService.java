package com.diginationmea.blogimo.auth;

import com.diginationmea.blogimo.auth.dto.TokenResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final String BLACKLIST = "blacklist:";

    private final StringRedisTemplate redis;
    private final JwtEncoder encoder;
    private final JwtDecoder accessDecoder;
    private final JwtDecoder refreshDecoder;

    JwtService(StringRedisTemplate redis, @Value("${app.jwt.secret}") String secret) {
        this.redis = redis;
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.accessDecoder = decoder(key, "access");
        this.refreshDecoder = decoder(key, "refresh");
    }

    public JwtDecoder accessDecoder() {
        return accessDecoder;
    }

    TokenResponse issue(Long userId) {
        return new TokenResponse(sign(userId, "access", ACCESS_TTL), sign(userId, "refresh", REFRESH_TTL));
    }

    Jwt decodeRefresh(String token) {
        return refreshDecoder.decode(token);
    }

    boolean revoke(Jwt jwt) {
        Duration ttl = Duration.between(Instant.now(), jwt.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) return true;
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(BLACKLIST + jwt.getId(), "1", ttl));
    }

    private String sign(Long userId, String type, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("type", type)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private JwtDecoder decoder(SecretKey key, String type) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                jwt -> type.equals(jwt.getClaimAsString("type")) && !Boolean.TRUE.equals(redis.hasKey(BLACKLIST + jwt.getId()))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return decoder;
    }
}
