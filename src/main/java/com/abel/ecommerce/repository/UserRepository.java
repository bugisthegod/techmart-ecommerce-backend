package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Find User By username for login
    Optional<User> findByUsername(String username);

    // Find User By email
    Optional<User> findByEmail(String email);

    // Check if username exists (for registration validation);
    boolean existsByUsername(String username);

    // Check if email exists (for registration validation)
    boolean existsByEmail(String email);

    // Find active users only
    Optional<User> FindByUsernameAndStatus(String username, Integer status);

}
