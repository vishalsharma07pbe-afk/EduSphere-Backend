package com.edusphere.identity.auth.controller;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;
import com.edusphere.identity.auth.model.AuthenticationResult;
import com.edusphere.identity.auth.refreshtoken.cookie.RefreshTokenCookieService;
import com.edusphere.identity.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService cookieService;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieService cookieService
    ) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticationResult result =
                authService.login(request);

        ResponseCookie refreshCookie =
                cookieService.createCookie(
                        result.rawRefreshToken()
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            HttpServletRequest request
    ) {
        String rawRefreshToken =
                cookieService.extractToken(request);

        AuthenticationResult result =
                authService.refresh(rawRefreshToken);

        ResponseCookie replacementCookie =
                cookieService.createCookie(
                        result.rawRefreshToken()
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        replacementCookie.toString()
                )
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {
        String rawRefreshToken =
                cookieService.extractToken(request);

        authService.logout(rawRefreshToken);

        ResponseCookie clearedCookie =
                cookieService.clearCookie();

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearedCookie.toString()
                )
                .build();
    }
}