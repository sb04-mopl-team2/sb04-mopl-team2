package com.codeit.mopl.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {
    @Getter
    @Value("${jwt.key}")
    private String secretKey;
    @Getter
    @Value("${jwt.access-token-expiration-minutes}")
    private int accessTokenExpirationMinutes;
    @Getter
    @Value("${jwt.refresh-token-expiration-minutes}")
    private int refreshTokenExpirationMinutes;

    public String generateAccessToken(Map<String, Object> claims, String subject) {
        log.info("AccessToken 발급 시도 subject = {}", subject);
        try {
            JWSSigner signer = new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8));

            Date expiration = new Date(System.currentTimeMillis() + accessTokenExpirationMinutes * 1000 * 60);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("userId", claims.get("userId"))
                    .claim("roles", claims.get("roles"))
                    .claim("type", "access")
                    .expirationTime(expiration)
                    .issueTime(new Date())
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );
            signedJWT.sign(signer);
            log.info("AccessToken 발급 성공 subject = {}", subject);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("JWT 발급 실패", e);
        }
    }

    public String generateRefreshToken(Map<String, Object> claims, String subject) {
        log.info("RefreshToken 발급 시도 subject = {}", subject);
        try {
            JWSSigner signer = new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8));

            Date expiration = new Date(System.currentTimeMillis() + refreshTokenExpirationMinutes * 60 * 1000);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("userId", claims.get("userId"))
                    .claim("roles", claims.get("roles"))
                    .claim("type", "refresh")
                    .expirationTime(expiration)
                    .issueTime(new Date())
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );
            signedJWT.sign(signer);
            log.info("RefreshToken 발급 성공 subject = {}", subject);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("JWT 발급 실패", e);
        }
    }

    public Map<String, Object> getClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("JWT 검증 실패");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return claimsSet.getClaims();
        } catch (Exception e) {
            throw new RuntimeException("JWT 파싱 실패", e);
        }
    }

    public void verifyJws(String jws) {
        log.info("JWS 검증 시도");
        try {
            SignedJWT jwt = SignedJWT.parse(jws);
            jwt.verify(new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8)));
            if (jwt.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new ExpiredJWTException("Access Token Expired");
            }
            log.info("JWS 검증 성공");
        } catch (Exception e) {
            throw new RuntimeException("JWT 검증 실패", e);
        }
    }

    public String getEmail(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        String email = jwt.getJWTClaimsSet()
                .getClaim("sub").toString();
        return email;
    }

    public UUID getUserId(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        UUID userId = UUID.fromString(
                jwt.getJWTClaimsSet()
                        .getClaim("userId")
                        .toString());
        return userId;
    }

    public boolean isExpired(String token) {
        log.info("Token 유효기간 검증 시도");
        try {
            Date now = new Date();
            SignedJWT jwt = SignedJWT.parse(token);
            if (jwt.getJWTClaimsSet().getExpirationTime().before(now)) {
                log.info("Token의 유효기간 만료 subject = {}", jwt.getJWTClaimsSet().getSubject());
                return true;
            }
            log.info("Token의 유효기간 검증 성공 subject = {}", jwt.getJWTClaimsSet().getSubject());
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Token의 유효기간 검증 중 오류 발생", e);
        }
    }

    public boolean validateAccessToken(String token) throws JOSEException {
        JWSVerifier accessTokenVerifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));
        return validateToken(token, accessTokenVerifier, "access");
    }

    public boolean validateRefreshToken(String token) throws JOSEException {
        JWSVerifier refreshTokenVerifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));
        return validateToken(token, refreshTokenVerifier, "refresh");
    }

    private boolean validateToken(String token, JWSVerifier verifier, String expectedType) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                log.debug("JWT signature verification failed for {} token", expectedType);
                return false;
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                log.debug("JWT token type mismatch: expected {}, got {}", expectedType, tokenType);
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.debug("JWT {} token expired", expectedType);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("JWT {} token validation failed: {}", expectedType, e.getMessage());
            return false;
        }
    }
}
