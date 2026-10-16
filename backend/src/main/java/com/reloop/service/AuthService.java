package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.Enums.Role;
import com.reloop.entity.User;
import com.reloop.exception.ApiException;
import com.reloop.repository.UserRepository;
import com.reloop.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole().name());
    }

    /** Public registration ALWAYS creates a CUSTOMER. A role in the request body is not even bound. */
    @Transactional
    public User register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists");
        User u = new User();
        u.setEmail(email);
        u.setFullName(r.fullName().trim());
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(Role.CUSTOMER);
        return users.save(u);
    }

    public User authenticate(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(() -> new BadCredentialsException("bad"));
        if (!u.isEnabled() || !encoder.matches(r.password(), u.getPasswordHash())) throw new BadCredentialsException("bad");
        return u;
    }

    public AuthResponse issue(User u) { return new AuthResponse(jwt.createAccessToken(u), toDto(u)); }

    public User fromRefreshToken(String token) {
        if (token == null || token.isBlank()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired");
        try {
            long id = Long.parseLong(jwt.parse(token, "refresh").getSubject());
            return users.findById(id).filter(User::isEnabled)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired"));
        } catch (JwtException | NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired");
        }
    }
}
