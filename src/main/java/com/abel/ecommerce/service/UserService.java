package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.UserLoginRequest;
import com.abel.ecommerce.dto.request.UserRegisterRequest;
import com.abel.ecommerce.dto.response.LoginResponse;
import com.abel.ecommerce.dto.response.UserResponse;
import com.abel.ecommerce.entity.Role;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.exception.IncorrectPasswordException;
import com.abel.ecommerce.exception.RoleNotFoundException;
import com.abel.ecommerce.exception.UserAlreadyExistsException;
import com.abel.ecommerce.exception.UserNotFoundException;
import com.abel.ecommerce.repository.RoleRepository;
import com.abel.ecommerce.repository.UserRepository;
import com.abel.ecommerce.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public User register(UserRegisterRequest request) {

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw UserAlreadyExistsException.username(request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserAlreadyExistsException.email(request.getEmail());
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(User.ACTIVE_USER); // Active status

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new RoleNotFoundException("CUSTOMER"));
        user.getRoles().add(customerRole);

        return userRepository.save(user);
    }

    public LoginResponse login(UserLoginRequest request) throws Exception {
        User user = findByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IncorrectPasswordException(user.getUsername());
        }
        String token = JwtTokenUtil.generateToken(user.getUsername());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setExpiresIn(JwtTokenUtil.getExpirationTimeInSeconds());

        UserResponse userInfo = new UserResponse();
        BeanUtils.copyProperties(user, userInfo);
        loginResponse.setUserInfo(userInfo);
        return loginResponse;

    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username, "username"));
    }

    /**
     * Logout user by blacklisting the provided token
     * @param token JWT token to invalidate
     */
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        // Validate token format and extract username for logging
        try {
            String username = JwtTokenUtil.getUsernameFromToken(token);
            tokenBlacklistService.blacklistToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid token provided for logout", e);
        }
    }

}
