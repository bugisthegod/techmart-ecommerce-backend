package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.entity.Role;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.exception.IncorrectPasswordException;
import com.abel.ecommerce.exception.RoleNotFoundException;
import com.abel.ecommerce.exception.UserAlreadyExistsException;
import com.abel.ecommerce.exception.UserNotFoundException;
import com.abel.ecommerce.repository.RoleRepository;
import com.abel.ecommerce.repository.UserRepository;
import com.abel.ecommerce.service.impl.UserServiceImpl;
import com.abel.ecommerce.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRegisterRequest testRegisterRequest;
    private UserLoginRequest testLoginRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword"); // Encoded password
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setStatus(User.ACTIVE_USER);

        // Create customer role
        customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setCode(Role.ROLE_CUSTOMER);
        customerRole.setName("Customer");

        // Create test register request
        testRegisterRequest = new UserRegisterRequest();
        testRegisterRequest.setUsername("newuser");
        testRegisterRequest.setPassword("password123");
        testRegisterRequest.setEmail("newuser@example.com");
        testRegisterRequest.setPhone("0987654321");

        // Create test login request
        testLoginRequest = new UserLoginRequest();
        testLoginRequest.setUsername("testuser");
        testLoginRequest.setPassword("password123");
    }

    // ========== REGISTER TESTS ==========

    @Test
    @DisplayName("Should register new user successfully")
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(roleRepository.findByCode(Role.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        User result = userService.register(testRegisterRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getStatus()).isEqualTo(User.ACTIVE_USER);
        assertThat(result.getUserRoles()).isNotEmpty();

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(roleRepository, times(1)).findByCode(Role.ROLE_CUSTOMER);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void register_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(testRegisterRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("newuser");

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(testRegisterRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("newuser@example.com");

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when customer role not found")
    void register_RoleNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(roleRepository.findByCode(Role.ROLE_CUSTOMER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.register(testRegisterRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining(Role.ROLE_CUSTOMER);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password when registering")
    void register_PasswordEncoded() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(roleRepository.findByCode(Role.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.register(testRegisterRequest);

        // Assert
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPassword");
        verify(passwordEncoder, times(1)).encode("password123");
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void login_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);

        // Act
        LoginResponse result = userService.login(testLoginRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getExpiresIn()).isGreaterThan(0);
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", testUser.getPassword());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        testLoginRequest.setUsername("nonexistent");

        // Act & Assert
        assertThatThrownBy(() -> userService.login(testLoginRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("nonexistent");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void login_IncorrectPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        testLoginRequest.setPassword("wrongpassword");

        // Act & Assert
        assertThatThrownBy(() -> userService.login(testLoginRequest))
                .isInstanceOf(IncorrectPasswordException.class)
                .hasMessageContaining("testuser");

        verify(passwordEncoder, times(1)).matches("wrongpassword", testUser.getPassword());
    }

    @Test
    @DisplayName("Should include user info in login response")
    void login_IncludesUserInfo() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);

        // Act
        LoginResponse result = userService.login(testLoginRequest);

        // Assert
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");
        assertThat(result.getUserInfo().getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUserInfo().getPhone()).isEqualTo("1234567890");
    }

    // ========== FIND BY USERNAME TESTS ==========

    @Test
    @DisplayName("Should find user by username successfully")
    void findByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findByUsername("testuser");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void findByUsername_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByUsername("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("nonexistent");

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @DisplayName("Should logout successfully with valid token")
    void logout_Success() throws Exception {
        // Arrange
        String validToken = JwtTokenUtil.generateToken(testUser.getUsername());
        doNothing().when(tokenBlacklistService).blacklistToken(validToken);

        // Act
        userService.logout(validToken);

        // Assert
        verify(tokenBlacklistService, times(1)).blacklistToken(validToken);
    }

    @Test
    @DisplayName("Should throw exception when logout with null token")
    void logout_NullToken_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.logout(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token cannot be null or empty");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when logout with empty token")
    void logout_EmptyToken_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.logout("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token cannot be null or empty");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when logout with invalid token format")
    void logout_InvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act & Assert
        assertThatThrownBy(() -> userService.logout(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid token");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should set user status to active when registering")
    void register_SetsActiveStatus() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(roleRepository.findByCode(Role.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.register(testRegisterRequest);

        // Assert
        assertThat(result.getStatus()).isEqualTo(User.ACTIVE_USER);
    }

    @Test
    @DisplayName("Should assign customer role when registering")
    void register_AssignsCustomerRole() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(roleRepository.findByCode(Role.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.register(testRegisterRequest);

        // Assert
        assertThat(result.getUserRoles()).isNotEmpty();
        assertThat(result.getUserRoles().get(0).getRole().getCode()).isEqualTo(Role.ROLE_CUSTOMER);
    }
}