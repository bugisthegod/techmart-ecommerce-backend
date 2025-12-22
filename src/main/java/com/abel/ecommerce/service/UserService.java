package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.entity.User;

public interface UserService {

    /**
     * Register a new user
     *
     * @param request User registration request
     * @return Registered user
     */
    User register(UserRegisterRequest request);

    /**
     * Login user
     *
     * @param request User login request
     * @return Login response with token and user info
     * @throws Exception if login fails
     */
    LoginResponse login(UserLoginRequest request);

    /**
     * Find user by username
     *
     * @param username Username to search for
     * @return User entity
     */
    User findByUsername(String username);

    /**
     * Logout user by blacklisting the provided token
     *
     * @param token JWT token to invalidate
     */
    void logout(String token);
}
