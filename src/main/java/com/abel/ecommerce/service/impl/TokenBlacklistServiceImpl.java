package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.service.TokenBlacklistService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    // Cache key prefix for blacklisted tokens
    private static final String BLACKLIST_KEY = "token:blacklist:";

    @Override
    public void blacklistToken(String token) {
        try {
            // Decode token to get expiration time
            DecodedJWT decodedJWT = JWT.decode(token);
            Date expiration = decodedJWT.getExpiresAt();

            if (expiration != null) {
                // Calculate remaining time until token expires
                long remainingTime = expiration.getTime() - System.currentTimeMillis();

                if (remainingTime > 0) {
                    // Store token in blacklist with remaining expiration time
                    String key = BLACKLIST_KEY + token;
                    redisTemplate.opsForValue().set(
                        key,
                        "blacklisted",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                    );
                    log.info("Token blacklisted successfully");
                } else {
                    log.warn("Token is already expired, no need to blacklist");
                }
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to blacklist token");
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check token blacklist status: {}", e.getMessage(), e);
            // On error, assume token is not blacklisted to avoid blocking valid users
            return false;
        }
    }

    @Override
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_KEY + token;
            redisTemplate.delete(key);
            log.info("Token removed from blacklist");
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", e.getMessage(), e);
        }
    }
}
