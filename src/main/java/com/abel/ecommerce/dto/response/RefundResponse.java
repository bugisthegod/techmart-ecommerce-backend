package com.abel.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private String refundId;
    private BigDecimal amount;
    private String status;
    private String message;
}
