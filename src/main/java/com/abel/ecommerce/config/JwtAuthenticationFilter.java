package com.abel.ecommerce.config;

import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.abel.ecommerce.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRoleCacheService userRoleCacheService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (!StringUtils.isEmpty(token)) {
            try {
                // Check if token is blacklisted
                if (tokenBlacklistService.isTokenBlacklisted(token)) {
                    logger.debug("Token is blacklisted, authentication denied");
                } else if (JwtTokenUtil.validateToken(token)) {
                    // Extract username from token
                    String username = JwtTokenUtil.getUsernameFromToken(token);

                    // Get role from cache or database
                    Set<String> userRoles = userRoleCacheService.getUserRoles(username);

                    // Create Spring Security authorities
                    List<SimpleGrantedAuthority> authorities = userRoles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();

                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            catch (Exception e) {
                // Token is invalid, continue without authentication
                logger.debug("JWT token validation failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}