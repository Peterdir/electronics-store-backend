package com.ecommerce.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm!!}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/brands/**").permitAll()
                        .requestMatchers("/api/inventory/**").permitAll()
                        .requestMatchers("/api/policies/**").permitAll()

                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/orders/checkout").permitAll()

                        .requestMatchers("/api/payments/vnpay/**").permitAll()

                        .anyRequest().authenticated())
                        .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .bearerTokenResolver(request -> {
                                    String path = request.getRequestURI();
                                    if (path.startsWith("/api/auth/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/uploads/")) {
                                        return null;
                                    }
                                    DefaultBearerTokenResolver resolver =
                                            new DefaultBearerTokenResolver();
                                    return resolver.resolve(request);
                                })
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder(StringRedisTemplate redisTemplate) {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

        OAuth2TokenValidator<Jwt> defaultValidator = new JwtTimestampValidator();

        OAuth2TokenValidator<Jwt> blacklistValidator = token -> {
            String tokenValue = token.getTokenValue();

            String isBlacklisted = redisTemplate.opsForValue().get("blacklist:" + tokenValue);

            if (isBlacklisted != null) {
                OAuth2Error error = new OAuth2Error("invalid_token", "This token has been logged out", null);
                return OAuth2TokenValidatorResult.failure(error);
            }
            return OAuth2TokenValidatorResult.success();
        };

        OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator,
                blacklistValidator);

        jwtDecoder.setJwtValidator(delegatingValidator);
        return jwtDecoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secretKey);
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}