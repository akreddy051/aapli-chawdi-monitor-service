package com.example.aapliChawdi.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

@Configuration
public class SecurityConfiguration {
    // Suppress Boot's generated default user: this service authenticates explicit secret headers only.
    @Bean
    AuthenticationManager authenticationManager() {
        return authentication -> { throw new BadCredentialsException("Unsupported authentication"); };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSecrets secrets) throws Exception {
        var webhook = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/telegram/webhook");
        return http
                // Authentication uses explicit headers only, never cookies, Basic auth or sessions.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(webhook).hasRole("TELEGRAM")
                        .requestMatchers(HttpMethod.POST, "/notices/trigger", "/notices/trigger/{villageId}",
                                "/villages/scrape", "/api/subscriptions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/subscriptions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/{chatId}", "/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
                        .hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> error(response, 401, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) -> error(response, 403, "Forbidden")))
                .addFilterBefore(new SecretAuthenticationFilter(secrets, webhook), AnonymousAuthenticationFilter.class)
                .build();
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + message + "\"}");
    }
}
