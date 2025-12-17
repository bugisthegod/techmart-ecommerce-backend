package com.abel.ecommerce.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddressResponse {

    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String postalCode;
    private String detailAddress;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields for convenience
    private String fullAddress;
    private boolean defaultAddress;
}
