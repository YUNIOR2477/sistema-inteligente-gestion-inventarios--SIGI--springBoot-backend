package com.sigi.configuration.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiresSeconds}")
    private long accessExpiresSeconds;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Authentication authentication, HttpServletRequest request) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        Objects.requireNonNull(principal, "Principal no puede ser nulo");

        final Date now = new Date();
        final Date exp = new Date(now.getTime() + accessExpiresSeconds * 1000L);

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .toList();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", roles);
        extraClaims.put("jti", UUID.randomUUID().toString());
        extraClaims.put("ip", request != null ? request.getRemoteAddr() : "unknown");

        return Jwts.builder()
                .claims(extraClaims)
                .subject(principal.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .signWith(getKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails expectedUser) {
        try {
            Claims claims = parseClaims(token);

            String subject = claims.getSubject();
            if (expectedUser != null && (subject == null || !subject.equals(expectedUser.getUsername()))) {
                return false;
            }

            Date expiration = claims.getExpiration();
            return expiration != null && !expiration.before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserName(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            Claims claims = parseClaims(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?>) {
                return ((List<?>) rolesObj).stream().map(String::valueOf).toList();
            }
            if (rolesObj instanceof String s) {
                return Arrays.stream(s.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList();
            }
            return Collections.emptyList();
        } catch (JwtException e) {
            return Collections.emptyList();
        }
    }

    public String extractJti(String token) {
        try {
            return parseClaims(token).get("jti", String.class);
        } catch (JwtException e) {
            return null;
        }
    }
}
