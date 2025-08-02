package com.abel.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class UserRegisterRequest {

    @Schema(description = "Username (3-50 characters)", example = "Abel_doe")
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @Schema(description = "Password (6-20 characters)", example = "password123")
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "Email address", example = "abel@example.com")
    @NotBlank(message = "Email cannot be empty")
    @Size(message = "Invalid email format")
    private String email;

    private String phone;

}
