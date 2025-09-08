package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleCacheService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    // Cache key prefix for user roles
    private static final String USER_ROLE_KEY = "user:roles:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * getUserRoles (priority from cache)
     *
     * @param username Username to query
     * @return set of role codes
     */

    public Set<String> getUserRoles(String username) {
        String key = USER_ROLE_KEY + username;
        String roleStr = redisTemplate.opsForValue().get("key");

        // get roles from cache
        if (!StringUtils.isEmpty(roleStr)) {
            log.debug("Get user roles from cache:{}", username);
            return Arrays.stream(roleStr.split(",")).collect(Collectors.toSet());
        }

        // Cache miss, load from database
        return loadAndCacheUserRoles(username);
    }

    /**
     * Load user roles from database and cache them
     * @param username
     * @return
     */
    private Set<String> loadAndCacheUserRoles(String username) {
        // Query user roles from database
        List<String> roles = userRepository.findRoleCodesByUsername(username);

        if (!roles.isEmpty()) {
            // Store user roles in redis
            String rolesStr = String.join(",", roles);
            redisTemplate.opsForValue().set(
                    USER_ROLE_KEY + username,
                    rolesStr,
                    CACHE_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );

        }

        return new HashSet<>(roles);
    }

    /**
     * Clear user rolr cache (call when roles change)
     * @param username
     */
    public void clearUserRoleCache(String username) {
        redisTemplate.delete(USER_ROLE_KEY + username);
        log.info("Cleared role cache for user: {}", username);
    }

}
