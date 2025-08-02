package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.dto.response.UserResponse;
import com.abel.ecommerce.entity.User;
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
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
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
            return ResponseResult.error(e.getCode(), e.getMessage());
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
            return ResponseResult.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ResponseResult<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        try {
            LoginResponse loginResponse = userService.login(request);
            return ResponseResult.ok(loginResponse);
        }
        catch (UserNotFoundException e) {
            return ResponseResult.error(e.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "A test api", description = "Test UserController")
    @GetMapping("/test")
    public ResponseResult<String> test() {
        return ResponseResult.ok("User Api is working");
    }

}
