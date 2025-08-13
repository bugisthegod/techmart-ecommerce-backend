package com.abel.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Address ID cannot be null")
    private Long addressId;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;

    // Optional: If user wants to use different freight calculation
    private String freightType; // "standard", "express", "same_day"
}
