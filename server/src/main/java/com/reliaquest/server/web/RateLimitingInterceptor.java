package com.reliaquest.server.web;

import com.reliaquest.server.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that applies rate limiting to HTTP requests using configurable limits.
 * Uses client IP address as the identifier for rate limiting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String clientId = getClientIdentifier(request);
        String requestPath = request.getRequestURI();

        log.debug("Processing rate limit check for client: {} on path: {}", clientId, requestPath);

        if (!rateLimitingService.isRequestAllowed(clientId, requestPath)) {
            handleRateLimitExceeded(response, requestPath);
            return false;
        }

        return true;
    }

    /**
     * Extracts client identifier from the request.
     * Uses X-Forwarded-For header if available, otherwise falls back to remote address.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Handles the case when rate limit is exceeded by setting appropriate response.
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String requestPath) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorMessage = rateLimitingService.getRateLimitErrorMessage(requestPath);
        String jsonResponse = String.format(
                "{\"status\":\"error\",\"message\":\"%s\",\"code\":%d}",
                errorMessage, HttpStatus.TOO_MANY_REQUESTS.value());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.warn("Rate limit exceeded for path: {}", requestPath);
    }
}
