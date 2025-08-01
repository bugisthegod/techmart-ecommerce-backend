package com.abel.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/users/register").permitAll()  // Allow registration without authentication
                        .requestMatchers("/api/users/login").permitAll()     // Allow login without authentication
                        .requestMatchers("/api/users/test").permitAll()      // Allow test endpoint without authentication
                        .anyRequest().authenticated()  // All other requests require authentication
                );
        return http.build();
    }
}
