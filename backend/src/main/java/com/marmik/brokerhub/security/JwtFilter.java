package com.marmik.brokerhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (StringUtils.hasText(header) && header.toLowerCase().startsWith("bearer ")) {
                // "Bearer " length is 7
                String token = header.substring(7).trim();

                if (jwtUtil.validateToken(token)) {
                    Optional<String> userIdOpt = jwtUtil.getUserId(token);

                    if (userIdOpt.isPresent()
                            && SecurityContextHolder.getContext().getAuthentication() == null) {

                        String userId = userIdOpt.get();

                        // Minimal authority: ROLE_USER. Per-account roles must be checked per-request.
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }

            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Don't run this filter for:
     * - auth endpoints (login)
     * - signup (creating new accounts)
     * - public/static resources
     * - preflight OPTIONS requests
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // allow OPTIONS for CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method))
            return true;

        // skip filtering for public endpoints
        return Arrays.stream(SecurityConstants.PUBLIC_ENDPOINTS)
                .anyMatch(path::startsWith);
    }
}
