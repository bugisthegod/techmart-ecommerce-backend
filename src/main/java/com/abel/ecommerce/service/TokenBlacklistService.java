package com.abel.ecommerce.service;

public interface TokenBlacklistService {

    /**
     * Add token to blacklist
     * @param token JWT token to blacklist
     */
    void blacklistToken(String token);

    /**
     * Check if token is blacklisted
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    boolean isTokenBlacklisted(String token);

    /**
     * Remove token from blacklist (mainly for testing purposes)
     * @param token JWT token to remove from blacklist
     */
    void removeFromBlacklist(String token);
}
