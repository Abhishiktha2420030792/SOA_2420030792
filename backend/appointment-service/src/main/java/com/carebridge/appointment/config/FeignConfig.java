package com.carebridge.appointment.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the caller's Authorization header onto outbound Feign calls,
 * so that when appointment-service calls provider-service (service-to-service),
 * the downstream service's JWT filter still sees a valid token and can
 * enforce its own role checks (zero-trust / defense in depth).
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authHeaderInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
