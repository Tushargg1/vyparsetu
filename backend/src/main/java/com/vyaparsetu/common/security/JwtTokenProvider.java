package com.vyaparsetu.common.security;

import com.vyaparsetu.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtTokenProvider(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = props.getSecurity().getJwt().getAccessTokenTtlMinutes();
    }

    public String generateAccessToken(Long userId, String uuid, String phone, Set<String> roles,
                                      Instant authenticatedAt, String authMethod) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("uuid", uuid)
                .claim("phone", phone)
                .claim("roles", String.join(",", roles))
                .claim("auth_time", authenticatedAt.getEpochSecond())
                .claim("amr", authMethod)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public AppPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String rolesStr = claims.get("roles", String.class);
        Set<String> roles = (rolesStr == null || rolesStr.isBlank())
                ? Set.of()
                : List.of(rolesStr.split(",")).stream().collect(Collectors.toSet());
        Object authTimeClaim = claims.get("auth_time");
        Instant authenticatedAt = authTimeClaim instanceof Number number
                ? Instant.ofEpochSecond(number.longValue()) : null;
        return new AppPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("uuid", String.class),
                claims.get("phone", String.class),
                roles,
                authenticatedAt,
                claims.get("amr", String.class)
        );
    }
}
