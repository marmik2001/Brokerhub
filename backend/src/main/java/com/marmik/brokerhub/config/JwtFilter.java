package com.marmik.brokerhub.config;

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
                String token = header.substring(6).trim();

                if (jwtUtil.validateToken(token)) {
                    Optional<String> memberIdOpt = jwtUtil.getMemberId(token);
                    Optional<String> roleOpt = jwtUtil.getRole(token);
                    Optional<String> accountIdOpt = jwtUtil.getAccountId(token);

                    if (memberIdOpt.isPresent() && roleOpt.isPresent()
                            && SecurityContextHolder.getContext().getAuthentication() == null) {

                        String memberId = memberIdOpt.get();
                        String role = roleOpt.get();

                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        var auth = new UsernamePasswordAuthenticationToken(memberId, null, authorities);
                        accountIdOpt.ifPresent(aid -> request.setAttribute("accountId", aid));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } else
                    SecurityContextHolder.clearContext();

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

        // endpoints that must be accessible without a token
        if (path.startsWith("/api/auth")
                || path.equals("/api/accounts/signup")
                || path.startsWith("/public")
                || path.startsWith("/static")) {
            return true;
        }

        return false;
    }
}
