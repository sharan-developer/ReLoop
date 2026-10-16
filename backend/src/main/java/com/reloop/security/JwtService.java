package com.reloop.security;

import com.reloop.config.AppProperties;
import com.reloop.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties props) {
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            log.warn("JWT_SECRET is missing or shorter than 32 chars: generating a random key. "
                    + "Tokens will be invalid after restart. Set JWT_SECRET for any real deployment.");
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.accessTtl = Duration.ofMinutes(props.getJwt().getAccessMinutes());
        this.refreshTtl = Duration.ofDays(props.getJwt().getRefreshDays());
    }

    public String createAccessToken(User u) { return build(u, "access", accessTtl); }

    public String createRefreshToken(User u) { return build(u, "refresh", refreshTtl); }

    public Duration refreshTtl() { return refreshTtl; }

    private String build(User u, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("type", type)
                .claim("role", u.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Returns the claims if signature and expiry are valid and the type matches; otherwise throws JwtException. */
    public Claims parse(String token, String expectedType) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!expectedType.equals(c.get("type", String.class))) throw new JwtException("wrong token type");
        return c;
    }
}
