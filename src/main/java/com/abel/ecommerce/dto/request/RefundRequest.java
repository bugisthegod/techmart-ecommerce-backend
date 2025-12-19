package com.abel.ecommerce.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRequest {

    @Size(max = 500, message = "Refund reason cannot exceed 500 characters")
    private String reason;
}
