package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order ID cannot be null")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotBlank(message = "Order number cannot be blank")
    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @NotNull(message = "Product ID cannot be null")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @NotBlank(message = "Product image cannot be blank")
    @Column(name = "product_image", nullable = false)
    private String productImage;

    @NotNull(message = "Product price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be positive")
    @Column(name = "product_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be positive")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Business methods
    public void calculateTotalAmount() {
        if (this.productPrice != null && this.quantity != null) {
            this.totalAmount = this.productPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    public boolean isValidQuantity() {
        return this.quantity != null && this.quantity > 0;
    }

    public boolean isValidPrice() {
        return this.productPrice != null && this.productPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
