package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Find User By username for login
    Optional<User> findByUsername(String username);

    // Find User By email
    Optional<User> findByEmail(String email);

    // Check if username exists (for registration validation);
    boolean existsByUsername(String username);

    // Check if email exists (for registration validation)
    boolean existsByEmail(String email);

    // Find active users only
    Optional<User> findByUsernameAndStatus(String username, Integer status);

    // Find role codes by username for caching
    @Query("SELECT r.code FROM User u JOIN u.roles r WHERE u.username = :username AND u.status = 1")
    List<String> findRoleCodesByUsername(@Param("username") String username);

}
