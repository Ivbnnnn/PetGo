package ru.mirea.petgo.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.mirea.petgo.config.SecurityConfig;
import ru.mirea.petgo.domain.AccessLevel;
import ru.mirea.petgo.dto.AuthDtos.ChangePassword;
import ru.mirea.petgo.dto.AuthDtos.Login;
import ru.mirea.petgo.dto.AuthDtos.Refresh;
import ru.mirea.petgo.dto.AuthDtos.Tokens;
import ru.mirea.petgo.dto.UserDto;
import ru.mirea.petgo.service.AuthService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public Tokens login(@Valid @RequestBody Login input, HttpServletResponse response) {
        return respond(auth.login(input), response);
    }

    @PostMapping("/refresh")
    public Tokens refresh(@Valid @RequestBody Refresh input, HttpServletResponse response) {
        return respond(auth.refresh(input.refreshToken()), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody Refresh input, HttpServletResponse response) {
        auth.logout(input.refreshToken());
        swaggerCookie(response, "", Duration.ZERO);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePassword input,
                               HttpServletResponse response) {
        auth.changePassword(UUID.fromString(jwt.getSubject()), input);
        swaggerCookie(response, "", Duration.ZERO);
    }

    private Tokens respond(Tokens tokens, HttpServletResponse response) {
        boolean admin = tokens.user().accessLevel() == AccessLevel.ADMIN && !tokens.user().passwordChangeRequired();
        swaggerCookie(response, admin ? tokens.accessToken() : "",
                admin ? Duration.between(Instant.now(), tokens.accessExpiresAt()) : Duration.ZERO);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return tokens;
    }

    private void swaggerCookie(HttpServletResponse response, String value, Duration age) {
        ResponseCookie cookie = ResponseCookie.from(SecurityConfig.SWAGGER_COOKIE, value)
                .httpOnly(true).secure(cookieSecure).sameSite("Strict").path("/")
                .maxAge(age.isNegative() ? Duration.ZERO : age).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
