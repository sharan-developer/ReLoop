package com.reloop.controller;

import com.reloop.config.AppProperties;
import com.reloop.dto.Dtos.*;
import com.reloop.entity.User;
import com.reloop.security.JwtService;
import com.reloop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Token strategy: short-lived access JWT returned in the JSON body (kept in memory by the SPA, never localStorage)
 * + long-lived refresh JWT in an HttpOnly, SameSite=Strict cookie scoped to /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    static final String COOKIE = "reloop_refresh";
    private final AuthService auth;
    private final JwtService jwt;
    private final AppProperties props;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r) {
        return respond(HttpStatus.CREATED, auth.register(r));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        return respond(HttpStatus.OK, auth.authenticate(r));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
        User u = auth.fromRefreshToken(token);
        return ResponseEntity.ok(auth.issue(u));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus s, User u) {
        return ResponseEntity.status(s)
                .header(HttpHeaders.SET_COOKIE, cookie(jwt.createRefreshToken(u), jwt.refreshTtl().getSeconds()).toString())
                .body(auth.issue(u));
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(props.isCookieSecure()).sameSite("Strict")
                .path("/api/auth").maxAge(maxAgeSeconds).build();
    }
}
