package com.example.aapliChawdi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Created only inside the security chain, never registered as a servlet filter bean.
final class SecretAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationSecrets secrets;
    private final RequestMatcher webhook;

    SecretAuthenticationFilter(AuthenticationSecrets secrets, RequestMatcher webhook) {
        this.secrets = secrets;
        this.webhook = webhook;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean telegramRequest = webhook.matches(request);
        String header = telegramRequest ? "X-Telegram-Bot-Api-Secret-Token" : "X-API-Key";
        List<String> values = Collections.list(request.getHeaders(header));
        String value = values.size() == 1 ? values.get(0) : null;
        boolean valid = telegramRequest ? secrets.matchesWebhook(value) : secrets.matchesAdmin(value);
        if (valid) {
            String role = telegramRequest ? "TELEGRAM" : "ADMIN";
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    role.toLowerCase(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
