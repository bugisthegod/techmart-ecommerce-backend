package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.dto.response.UserResponse;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.exception.IncorrectPasswordException;
import com.abel.ecommerce.exception.UserAlreadyExistsException;
import com.abel.ecommerce.exception.UserNotFoundException;
import com.abel.ecommerce.service.UserService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User registration, login and profile management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PostMapping("/register")
    public ResponseResult<String> register(@Valid @RequestBody UserRegisterRequest request) {
        try {
            User user = userService.register(request);
            return ResponseResult.ok("User registered successfully with ID: " + user.getId());
        }
        catch (UserAlreadyExistsException e) {
            log.error("User registration failed - user already exists: {}", e.getMessage(), e);
            return ResponseResult.error(e.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error during user registration", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Find user by username")
    @GetMapping("/{username}")
    public ResponseResult<UserResponse> findUserByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            UserResponse userResponse = new UserResponse();
            BeanUtils.copyProperties(user, userResponse);
            return ResponseResult.ok(userResponse);
        }
        catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage(), e);
            return ResponseResult.error(e.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error when finding user by username: {}", username, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ResponseResult<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        try {
            LoginResponse loginResponse = userService.login(request);
            return ResponseResult.ok(loginResponse);
        }
        catch (UserNotFoundException | IncorrectPasswordException e) {
            log.error("Login failed: {}", e.getMessage(), e);
            return ResponseResult.error(e.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "User logout", description = "Invalidate JWT token and logout user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged out successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing token"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid token")
    })
    @PostMapping("/logout")
    public ResponseResult<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Validate Authorization header format
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseResult.error(ResultCode.UNAUTHORIZED.getCode(), "Invalid Authorization header format");
            }

            // Extract token from Authorization header
            String token = authorizationHeader.substring(7);

            // Call service to logout (blacklist token)
            userService.logout(token);

            return ResponseResult.ok("User logged out successfully");
        }
        catch (IllegalArgumentException e) {
            log.error("Logout failed - invalid token: {}", e.getMessage(), e);
            return ResponseResult.error(ResultCode.PARAM_NOT_VALID.getCode(), e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("Logout failed - token validation error: {}", e.getMessage(), e);
            return ResponseResult.error(ResultCode.UNAUTHORIZED.getCode(), "Invalid token");
        }
        catch (Exception e) {
            log.error("Unexpected error during logout", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), "Logout failed");
        }
    }

    @Operation(summary = "A test api", description = "Test UserController")
    @GetMapping("/test")
    public ResponseResult<String> test() {
        return ResponseResult.ok("User Api is working");
    }

}
