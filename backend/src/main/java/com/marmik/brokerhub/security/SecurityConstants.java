package com.marmik.brokerhub.security;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    public static final String[] PUBLIC_ENDPOINTS = {
            // Auth & User registration
            "/api/auth/**",
            "/api/user/register",

            // Documentation & monitoring
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // Legacy or static/public routes
            "/public/**",
            "/static/**"
    };
}
