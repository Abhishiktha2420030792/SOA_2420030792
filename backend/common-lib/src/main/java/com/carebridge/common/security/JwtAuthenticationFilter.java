package com.carebridge.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Re-validates the JWT locally inside EVERY downstream microservice.
 *
 * The Gateway already blocks unauthenticated traffic, but each service
 * independently re-checks the token (defense in depth / zero-trust) and
 * builds a Spring Security Authentication with the role as a GrantedAuthority
 * ("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN") so @PreAuthorize works.
 *
 * The Gateway also forwards X-User-Id / X-User-Role headers after validating
 * the token so services can trust them directly, but we still re-parse the
 * raw Authorization header here for services called outside the Gateway
 * (e.g. service-to-service calls, local testing via Postman directly on a
 * service port).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.extractUserId(token);
                    Role role = jwtUtil.extractRole(token);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Make userId/role available to controllers without re-parsing the token
                    request.setAttribute("userId", userId);
                    request.setAttribute("role", role.name());
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
