package ru.mirea.petgo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.mirea.petgo.error.ApiError;
import ru.mirea.petgo.security.AccountAuthentication;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration
public class SecurityConfig {
    public static final String SWAGGER_COOKIE = "petgo_swagger";
    private static final String[] DOCS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};
    private static final String[] READ_LEVELS = {"VIEWER", "EDITOR", "ADMIN"};
    private static final String[] WRITE_LEVELS = {"EDITOR", "ADMIN"};

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret-base64}") String encoded) {
        byte[] raw = Base64.getDecoder().decode(encoded);
        if (raw.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET_BASE64 должен содержать не менее 32 случайных байт");
        }
        return new SecretKeySpec(raw, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(issuer), new JwtTimestampValidator(Duration.ZERO)));
        return decoder;
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip)
                .filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AccountAuthentication accounts, ObjectMapper json)
            throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, error) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(),
                    new ApiError(Instant.now(), 401, "Unauthorized", "Требуется действующая авторизация"));
        };
        AccessDeniedHandler forbidden = (request, response, error) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(),
                    new ApiError(Instant.now(), 403, "Forbidden", "Недостаточно прав для операции"));
        };
        DefaultBearerTokenResolver bearer = new DefaultBearerTokenResolver();
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                        .requestMatchers(DOCS).hasRole("ADMIN")
                        .requestMatchers("/api/admin/**", "/api/walk-requests/*/permanent").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/districts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/districts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/districts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pets", "/api/pets/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/pets", "/api/pets/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/pets", "/api/pets/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/pets", "/api/pets/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(READ_LEVELS)
                        .requestMatchers(HttpMethod.HEAD, "/api/**").hasAnyRole(READ_LEVELS)
                        .requestMatchers("/api/**").hasAnyRole(WRITE_LEVELS)
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .oauth2ResourceServer(jwt -> jwt
                        .bearerTokenResolver(request -> resolveToken(bearer, request))
                        .authenticationEntryPoint(unauthorized)
                        .jwt(config -> config.jwtAuthenticationConverter(accounts)))
                .build();
    }

    private static String resolveToken(DefaultBearerTokenResolver bearer, HttpServletRequest request) {
        String token = bearer.resolve(request);
        if (token != null || request.getCookies() == null) {
            return token;
        }
        for (String path : DOCS) {
            if (PathPatternRequestMatcher.withDefaults().matcher(path).matches(request)) {
                for (Cookie cookie : request.getCookies()) {
                    if (SWAGGER_COOKIE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }
}
