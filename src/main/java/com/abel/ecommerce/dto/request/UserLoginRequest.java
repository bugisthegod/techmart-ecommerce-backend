package com.abel.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "User login request")
public class UserLoginRequest {

    @Schema(description = "Username cannot by empty", example = "Abel")
    @NotBlank(message = "Username cannot be empty")
    private String username;

    @Schema(description = "Password cannot be empty", example = "password123")
    @NotBlank(message = "Password cannot be empty")
    private String password;

}
