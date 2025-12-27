package com.abel.ecommerce.controller;

import com.abel.ecommerce.config.JwtAuthenticationFilter;
import com.abel.ecommerce.config.SecurityConfig;
import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.dto.response.UserResponse;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.exception.GlobalExceptionHandler;
import com.abel.ecommerce.exception.IncorrectPasswordException;
import com.abel.ecommerce.exception.UserAlreadyExistsException;
import com.abel.ecommerce.exception.UserNotFoundException;
import com.abel.ecommerce.filter.RateLimitFilter;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.abel.ecommerce.service.UserService;
import com.abel.ecommerce.utils.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for UserController
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class})
@DisplayName("UserController Web Layer Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private UserRegisterRequest testRegisterRequest;
    private UserLoginRequest testLoginRequest;
    private LoginResponse testLoginResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Configure mock filters to pass through requests
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                (ServletRequest) invocation.getArgument(0),
                (ServletResponse) invocation.getArgument(1)
            );
            return null;
        }).when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                (ServletRequest) invocation.getArgument(0),
                (ServletResponse) invocation.getArgument(1)
            );
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setStatus(User.ACTIVE_USER);

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

        // Create test login response
        testLoginResponse = new LoginResponse();
        testLoginResponse.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");
        testLoginResponse.setExpiresIn(3600L);
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        testLoginResponse.setUserInfo(userResponse);
    }

    // ========== REGISTER TESTS ==========

    @Test
    @DisplayName("Should register new user successfully")
    void register_Success() throws Exception {
        // Arrange
        when(userService.register(any(UserRegisterRequest.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(containsString("User registered successfully")))
                .andExpect(jsonPath("$.data").value(containsString("1")));

        verify(userService, times(1)).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when username is blank")
    void register_BlankUsername_BadRequest() throws Exception {
        // Arrange
        testRegisterRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void register_InvalidEmail_BadRequest() throws Exception {
        // Arrange
        testRegisterRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void register_UsernameExists_Conflict() throws Exception {
        // Arrange
        when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists: newuser"));

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(ResultCode.USER_ACCOUNT_ALREADY_EXIST.getCode()));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void register_EmailExists_Conflict() throws Exception {
        // Arrange
        when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Email already exists: newuser@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void login_Success() throws Exception {
        // Arrange
        when(userService.login(any(UserLoginRequest.class))).thenReturn(testLoginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.userInfo.username").value("testuser"))
                .andExpect(jsonPath("$.data.userInfo.email").value("test@example.com"));

        verify(userService, times(1)).login(any(UserLoginRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when login with blank username")
    void login_BlankUsername_BadRequest() throws Exception {
        // Arrange
        testLoginRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(UserLoginRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void login_UserNotFound() throws Exception {
        // Arrange
        when(userService.login(any(UserLoginRequest.class)))
                .thenThrow(new UserNotFoundException("nonexistent", "username"));

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.USER_ACCOUNT_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("Should return 401 when password is incorrect")
    void login_IncorrectPassword() throws Exception {
        // Arrange
        when(userService.login(any(UserLoginRequest.class)))
                .thenThrow(new IncorrectPasswordException("testuser"));

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ResultCode.USER_CREDENTIALS_ERROR.getCode()));
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @DisplayName("Should logout successfully with valid token")
    @WithMockUser
    void logout_Success() throws Exception {
        // Arrange
        String token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        doNothing().when(userService).logout(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/users/logout")
                        .with(csrf())
                        .header("Authorization", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("User logged out successfully"));

        verify(userService, times(1)).logout(anyString());
    }

    @Test
    @DisplayName("Should return error when logout without Authorization header")
    @WithMockUser
    void logout_NoAuthorizationHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/users/logout")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).logout(anyString());
    }

    @Test
    @DisplayName("Should return error when logout with invalid Authorization header format")
    @WithMockUser
    void logout_InvalidAuthorizationFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/users/logout")
                        .with(csrf())
                        .header("Authorization", "InvalidFormat token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ResultCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.msg").value(containsString("Invalid Authorization header format")));

        verify(userService, never()).logout(anyString());
    }

    // ========== FIND USER BY USERNAME TESTS ==========

    @Test
    @DisplayName("Should find user by username successfully")
    @WithMockUser
    void findUserByUsername_Success() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", "testuser"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.phone").value("1234567890"));

        verify(userService, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return 404 when user not found by username")
    @WithMockUser
    void findUserByUsername_NotFound() throws Exception {
        // Arrange
        when(userService.findByUsername("nonexistent"))
                .thenThrow(new UserNotFoundException("nonexistent", "username"));

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", "nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.USER_ACCOUNT_NOT_EXIST.getCode()));
    }

    // ========== TEST ENDPOINT TESTS ==========

    @Test
    @DisplayName("Should return success message from test endpoint")
    @WithMockUser
    void test_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("User Api is working"));
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate all required fields in register request")
    void register_MissingRequiredFields_ValidationError() throws Exception {
        // Arrange
        UserRegisterRequest invalidRequest = new UserRegisterRequest();
        // Missing all required fields

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("Should validate password minimum length")
    void register_ShortPassword_ValidationError() throws Exception {
        // Arrange
        testRegisterRequest.setPassword("123"); // Too short

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserRegisterRequest.class));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void register_ServiceException_ErrorResponse() throws Exception {
        // Arrange
        when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRegisterRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999));
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @DisplayName("Should handle JSON content type")
    void login_JsonContentType() throws Exception {
        // Arrange
        when(userService.login(any(UserLoginRequest.class))).thenReturn(testLoginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
