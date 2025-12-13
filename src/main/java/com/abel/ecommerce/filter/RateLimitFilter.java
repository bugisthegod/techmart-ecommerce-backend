package com.abel.ecommerce.filter;

import com.abel.ecommerce.utils.JwtTokenUtil;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String IP_RATE_LIMIT_KEY_PREFIX = "rate_limit:ip:";
    private static final String USER_RATE_LIMIT_KEY_PREFIX = "rate_limit:user:";

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration ipRateLimitConfig;
    private final BucketConfiguration userRateLimitConfig;

    public RateLimitFilter(
            ProxyManager<String> rateLimitProxyManager,
            @Qualifier("ipRateLimitConfig") BucketConfiguration ipRateLimitConfig,
            @Qualifier("userRateLimitConfig") BucketConfiguration userRateLimitConfig) {
        this.proxyManager = rateLimitProxyManager;
        this.ipRateLimitConfig = ipRateLimitConfig;
        this.userRateLimitConfig = userRateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract client IP
        String clientIp = getClientIp(request);

        // Check IP rate limit using Redis-backed bucket
        String ipKey = IP_RATE_LIMIT_KEY_PREFIX + clientIp;
        BucketProxy ipBucket = proxyManager.builder()
                .build(ipKey, () -> ipRateLimitConfig);

        if (!ipBucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests from IP. Please try again later.\"}");
            return;
        }

        // Attempt to extract userId from JWT token
        Long userId = extractUserIdFromToken(request);

        // If authenticated, check user rate limit using Redis-backed bucket
        if (userId != null) {
            String userKey = USER_RATE_LIMIT_KEY_PREFIX + userId;
            BucketProxy userBucket = proxyManager.builder()
                    .build(userKey, () -> userRateLimitConfig);

            if (!userBucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests from user. Please try again later.\"}");
                return;
            }
        }

        // Both checks passed, continue with the request
        filterChain.doFilter(request, response);
    }

    /**
     * Extract client IP from request, considering proxy headers
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Extract userId from JWT token if present
     */
    private Long extractUserIdFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return JwtTokenUtil.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            // Token parsing failed or not present, return null
        }
        return null;
    }
}