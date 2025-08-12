package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    // Default status constants
    public static final Integer DEFAULT_ADDRESS = 1;
    public static final Integer NON_DEFAULT_ADDRESS = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID cannot be null")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "Receiver name cannot be blank")
    @Size(max = 50, message = "Receiver name cannot exceed 50 characters")
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @NotBlank(message = "Receiver phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @NotBlank(message = "Province cannot be blank")
    @Size(max = 50, message = "Province name cannot exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String province;

    @NotBlank(message = "City cannot be blank")
    @Size(max = 50, message = "City name cannot exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String city;

    @NotBlank(message = "District cannot be blank")
    @Size(max = 50, message = "District name cannot exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String district;

    @NotBlank(message = "Detail address cannot be blank")
    @Size(max = 255, message = "Detail address cannot exceed 255 characters")
    @Column(name = "detail_address", nullable = false)
    private String detailAddress;

    @Column(name = "is_default", columnDefinition = "TINYINT DEFAULT 0")
    private Integer isDefault = NON_DEFAULT_ADDRESS; // 0-not default, 1-default

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business method: Get full address string
    public String getFullAddress() {
        return province + " " + city + " " + district + " " + detailAddress;
    }

    // Business method: Check if this is default address
    public boolean isDefaultAddress() {
        return isDefault != null && isDefault.equals(DEFAULT_ADDRESS);
    }
}
