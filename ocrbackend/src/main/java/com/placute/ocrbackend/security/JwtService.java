package com.placute.ocrbackend.security;

import com.placute.ocrbackend.model.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String configuredSecretKey;

    private Key signInKey;

    @PostConstruct
    void init() {
        this.signInKey = buildSignInKey(configuredSecretKey);
    }

    private Key getSignInKey() {
        return signInKey;
    }

    private Key buildSignInKey(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("JWT secret lipseste. Configureaza JWT_SECRET.");
        }

        byte[] keyBytes = decodeSecret(rawSecret.trim());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret este prea scurt. Foloseste minim 32 bytes.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecret(String rawSecret) {
        try {
            return Decoders.BASE64.decode(rawSecret);
        } catch (RuntimeException ignored) {
            try {
                return Decoders.BASE64URL.decode(rawSecret);
            } catch (RuntimeException ignoredToo) {
                return rawSecret.getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
