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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @GetMapping("/{username}")
    public ResponseResult<UserResponse> findUserByUsername(String username) {
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

    @GetMapping("/test")
    public ResponseResult<String> test() {
        return ResponseResult.ok("User Api is working");
    }

}
