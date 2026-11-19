package com.diginationmea.blogimo.auth;

import com.diginationmea.blogimo.auth.dto.LoginRequest;
import com.diginationmea.blogimo.auth.dto.RegisterRequest;
import com.diginationmea.blogimo.auth.dto.RegisterResponse;
import com.diginationmea.blogimo.auth.dto.TokenResponse;
import com.diginationmea.blogimo.user.User;
import com.diginationmea.blogimo.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    RegisterResponse register(RegisterRequest req) {
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPassword(encoder.encode(req.password()));
        user = users.save(user);
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail());
    }

    TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return jwt.issue(user.getId());
    }

    TokenResponse refresh(String refreshToken) {
        Jwt old;
        try {
            old = jwt.decodeRefresh(refreshToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        Long userId = Long.valueOf(old.getSubject());
        if (!jwt.revoke(old) || !users.existsById(userId)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        return jwt.issue(userId);
    }

    void logout(Jwt access, String refreshToken) {
        jwt.revoke(access);
        if (refreshToken == null) return;
        try {
            jwt.revoke(jwt.decodeRefresh(refreshToken));
        } catch (JwtException ignored) {
        }
    }
}
