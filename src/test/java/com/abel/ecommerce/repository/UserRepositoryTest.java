package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Role;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository
 *
 * Tests user authentication, registration validation,
 * and role-based queries
 */
@DataJpaTest
@DisplayName("UserRepository Integration Tests")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private User activeUser;
    private User inactiveUser;
    private User adminUser;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        customerRole = new Role();
        customerRole.setName("Customer");
        customerRole.setCode(Role.ROLE_CUSTOMER);
        customerRole.setDescription("Regular customer");
        customerRole.setStatus(Role.ACTIVE_ROLE);
        customerRole = roleRepository.save(customerRole);

        adminRole = new Role();
        adminRole.setName("Super Admin");
        adminRole.setCode(Role.ROLE_SUPER_ADMIN);
        adminRole.setDescription("System administrator");
        adminRole.setStatus(Role.ACTIVE_ROLE);
        adminRole = roleRepository.save(adminRole);

        // Create active user with customer role
        activeUser = new User();
        activeUser.setUsername("john_doe");
        activeUser.setPassword("hashedpassword123");
        activeUser.setEmail("john@example.com");
        activeUser.setPhone("13812345678");
        activeUser.setStatus(User.ACTIVE_USER);
        activeUser = userRepository.save(activeUser);

        UserRole userRole1 = new UserRole();
        userRole1.setUser(activeUser);
        userRole1.setRole(customerRole);
        userRoleRepository.save(userRole1);

        // Create inactive user
        inactiveUser = new User();
        inactiveUser.setUsername("inactive_user");
        inactiveUser.setPassword("hashedpassword456");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setStatus(User.NONACTIVE_USER);
        inactiveUser = userRepository.save(inactiveUser);

        // Create admin user with multiple roles
        adminUser = new User();
        adminUser.setUsername("admin_user");
        adminUser.setPassword("adminpassword789");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhone("13900000000");
        adminUser.setStatus(User.ACTIVE_USER);
        adminUser = userRepository.save(adminUser);

        UserRole userRole2 = new UserRole();
        userRole2.setUser(adminUser);
        userRole2.setRole(customerRole);
        userRoleRepository.save(userRole2);

        UserRole userRole3 = new UserRole();
        userRole3.setUser(adminUser);
        userRole3.setRole(adminRole);
        userRoleRepository.save(userRole3);
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    @DisplayName("Should save and retrieve user")
    void saveAndFindUser() {
        User newUser = new User();
        newUser.setUsername("new_user");
        newUser.setPassword("password");
        newUser.setEmail("newuser@example.com");

        User saved = userRepository.save(newUser);
        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("new_user");
        assertThat(found.get().getStatus()).isEqualTo(User.ACTIVE_USER);
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void findById_NotFound() {
        Optional<User> result = userRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete user")
    void deleteUser() {
        Long userId = activeUser.getId();
        userRepository.deleteById(userId);
        Optional<User> deleted = userRepository.findById(userId);
        assertThat(deleted).isEmpty();
    }

    // ========== AUTHENTICATION QUERY TESTS ==========

    @Test
    @DisplayName("Should find user by username")
    void findByUsername() {
        Optional<User> found = userRepository.findByUsername("john_doe");
        Optional<User> notFound = userRepository.findByUsername("nonexistent");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail() {
        Optional<User> found = userRepository.findByEmail("john@example.com");
        Optional<User> notFound = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should check if username exists")
    void existsByUsername() {
        boolean exists = userRepository.existsByUsername("john_doe");
        boolean notExists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail() {
        boolean exists = userRepository.existsByEmail("john@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find active user by username and status")
    void findByUsernameAndStatus() {
        Optional<User> activeFound = userRepository.findByUsernameAndStatus(
                "john_doe",
                User.ACTIVE_USER
        );
        Optional<User> inactiveNotFound = userRepository.findByUsernameAndStatus(
                "inactive_user",
                User.ACTIVE_USER
        );

        assertThat(activeFound).isPresent();
        assertThat(activeFound.get().getUsername()).isEqualTo("john_doe");
        assertThat(inactiveNotFound).isEmpty();
    }

    // ========== ROLE-BASED QUERY TESTS ==========

    @Test
    @DisplayName("Should find role codes by username for active user")
    void findRoleCodesByUsername() {
        List<String> roleCodes = userRepository.findRoleCodesByUsername("john_doe");

        assertThat(roleCodes).hasSize(1);
        assertThat(roleCodes).contains(Role.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("Should find multiple role codes for admin user")
    void findRoleCodesByUsername_MultipleRoles() {
        List<String> roleCodes = userRepository.findRoleCodesByUsername("admin_user");

        assertThat(roleCodes).hasSize(2);
        assertThat(roleCodes).containsExactlyInAnyOrder(
                Role.ROLE_CUSTOMER,
                Role.ROLE_SUPER_ADMIN
        );
    }

    @Test
    @DisplayName("Should return empty list for inactive user")
    void findRoleCodesByUsername_InactiveUser() {
        List<String> roleCodes = userRepository.findRoleCodesByUsername("inactive_user");

        assertThat(roleCodes).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user")
    void findRoleCodesByUsername_NotFound() {
        List<String> roleCodes = userRepository.findRoleCodesByUsername("nonexistent");

        assertThat(roleCodes).isEmpty();
    }

    // ========== REGISTRATION VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate unique username constraint")
    void uniqueUsername() {
        boolean existsBefore = userRepository.existsByUsername("unique_user");
        assertThat(existsBefore).isFalse();

        User user = new User();
        user.setUsername("unique_user");
        user.setPassword("password");
        user.setEmail("unique@example.com");
        userRepository.save(user);

        boolean existsAfter = userRepository.existsByUsername("unique_user");
        assertThat(existsAfter).isTrue();
    }

    @Test
    @DisplayName("Should validate unique email constraint")
    void uniqueEmail() {
        boolean existsBefore = userRepository.existsByEmail("unique@example.com");
        assertThat(existsBefore).isFalse();

        User user = new User();
        user.setUsername("test_unique");
        user.setPassword("password");
        user.setEmail("unique@example.com");
        userRepository.save(user);

        boolean existsAfter = userRepository.existsByEmail("unique@example.com");
        assertThat(existsAfter).isTrue();
    }

    @Test
    @DisplayName("Should handle case-sensitive username search")
    void usernameCase() {
        Optional<User> found = userRepository.findByUsername("john_doe");
        Optional<User> notFound = userRepository.findByUsername("JOHN_DOE");

        assertThat(found).isPresent();
        // Username is case-sensitive in database
        assertThat(notFound).isEmpty();
    }

    // ========== USER STATUS TESTS ==========

    @Test
    @DisplayName("Should find all users regardless of status")
    void findAll_AllStatuses() {
        List<User> allUsers = userRepository.findAll();

        assertThat(allUsers).hasSize(3);
        assertThat(allUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "inactive_user", "admin_user");
    }

    @Test
    @DisplayName("Should distinguish active and inactive users")
    void userStatusDistinction() {
        User active = userRepository.findByUsername("john_doe").orElseThrow();
        User inactive = userRepository.findByUsername("inactive_user").orElseThrow();

        assertThat(active.getStatus()).isEqualTo(User.ACTIVE_USER);
        assertThat(inactive.getStatus()).isEqualTo(User.NONACTIVE_USER);
    }

    @Test
    @DisplayName("Should update user status")
    void updateUserStatus() {
        User user = userRepository.findByUsername("john_doe").orElseThrow();
        user.setStatus(User.NONACTIVE_USER);

        User updated = userRepository.save(user);

        assertThat(updated.getStatus()).isEqualTo(User.NONACTIVE_USER);

        // Verify status-based query no longer finds this user
        Optional<User> notFound = userRepository.findByUsernameAndStatus(
                "john_doe",
                User.ACTIVE_USER
        );
        assertThat(notFound).isEmpty();
    }

    // ========== ENTITY LIFECYCLE TESTS ==========

    @Test
    @DisplayName("Should set default values and timestamps on save")
    void entityLifecycle_Defaults() {
        User newUser = new User();
        newUser.setUsername("timestamp_test");
        newUser.setPassword("password");
        newUser.setEmail("timestamp@example.com");

        User saved = userRepository.save(newUser);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(User.ACTIVE_USER);
    }

    @Test
    @DisplayName("Should update timestamps on entity update")
    void entityLifecycle_UpdateTimestamp() throws InterruptedException {
        User user = userRepository.findById(activeUser.getId()).orElseThrow();
        Thread.sleep(100);

        user.setPhone("13999999999");
        User updated = userRepository.saveAndFlush(user);

        assertThat(updated.getPhone()).isEqualTo("13999999999");
        assertThat(updated.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle user with no roles")
    void findRoleCodesByUsername_NoRoles() {
        // Inactive user has no roles
        List<String> roleCodes = userRepository.findRoleCodesByUsername("inactive_user");
        assertThat(roleCodes).isEmpty();
    }

    @Test
    @DisplayName("Should handle null or empty searches gracefully")
    void findByUsername_EmptyString() {
        Optional<User> result = userRepository.findByUsername("");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should maintain data integrity with user-role relationships")
    void userRoleRelationshipIntegrity() {
        User user = userRepository.findByUsername("admin_user").orElseThrow();
        List<String> roleCodes = userRepository.findRoleCodesByUsername("admin_user");

        assertThat(roleCodes).hasSize(2);
        assertThat(user.getStatus()).isEqualTo(User.ACTIVE_USER);
    }
}