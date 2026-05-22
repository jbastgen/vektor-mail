package com.vektor.mail.admin.controller;

import com.vektor.mail.admin.dto.LoginRequest;
import com.vektor.mail.admin.dto.LoginResponse;
import com.vektor.mail.core.model.Account;
import com.vektor.mail.security.crypto.PasswordEncoder;
import com.vektor.mail.security.jwt.JwtService;
import com.vektor.mail.storage.db.repository.AccountRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req,
                                                HttpServletResponse response) {
        Account account = accountRepository.findByEmail(req.email())
                .filter(a -> a.isActive() && passwordEncoder.verify(req.password(), a.getPasswordHash()))
                .orElse(null);

        if (account == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = jwtService.issueAccessToken(account.getEmail(), account.getRoles());
        String refreshToken = jwtService.issueRefreshToken(account.getEmail());

        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (30 * 24 * 3600));
        response.addCookie(cookie);

        return ResponseEntity.ok(new LoginResponse(accessToken, account.getEmail(), account.getRoles()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/api/auth/refresh");
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }
}
