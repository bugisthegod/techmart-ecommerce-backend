package com.abel.ecommerce.service;

import java.util.Set;

public interface UserRoleCacheService {

    /**
     * Get user roles (priority from cache)
     * @param username Username to query
     * @return Set of role codes
     */
    Set<String> getUserRoles(String username);

    /**
     * Clear user role cache (call when roles change)
     * @param username Username
     */
    void clearUserRoleCache(String username);
}
