package com.abel.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Receiver name cannot be blank")
    @Size(max = 50, message = "Receiver name cannot exceed 50 characters")
    private String receiverName;

    @NotBlank(message = "Receiver phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String receiverPhone;

    @NotBlank(message = "Province cannot be blank")
    @Size(max = 50, message = "Province name cannot exceed 50 characters")
    private String province;

    @NotBlank(message = "City cannot be blank")
    @Size(max = 50, message = "City name cannot exceed 50 characters")
    private String city;

    @NotBlank(message = "District cannot be blank")
    @Size(max = 50, message = "District name cannot exceed 50 characters")
    private String district;

    @NotBlank(message = "Detail address cannot be blank")
    @Size(max = 255, message = "Detail address cannot exceed 255 characters")
    private String detailAddress;

    private Integer isDefault;
}
