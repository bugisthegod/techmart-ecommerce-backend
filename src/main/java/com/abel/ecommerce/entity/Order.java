package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    // Order status constants
    public static final Integer STATUS_PENDING_PAYMENT = 0;  // Pending payment
    public static final Integer STATUS_PAID = 1;             // Paid
    public static final Integer STATUS_SHIPPED = 2;          // Shipped
    public static final Integer STATUS_COMPLETED = 3;        // Completed
    public static final Integer STATUS_CANCELLED = 4;        // Cancelled

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Order number cannot be blank")
    @Column(name = "order_no", unique = true, nullable = false, length = 32)
    private String orderNo;

    @NotNull(message = "User ID cannot be null")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be positive")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Pay amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Pay amount must be positive")
    @Column(name = "pay_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @DecimalMin(value = "0.0", message = "Freight amount cannot be negative")
    @Column(name = "freight_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @NotNull(message = "Order status cannot be null")
    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = STATUS_PENDING_PAYMENT;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "receive_time")
    private LocalDateTime receiveTime;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String comment;

    @NotBlank(message = "Receiver name cannot be blank")
    @Size(max = 50, message = "Receiver name cannot exceed 50 characters")
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @NotBlank(message = "Receiver phone cannot be blank")
    @Size(max = 20, message = "Receiver phone cannot exceed 20 characters")
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @NotBlank(message = "Receiver address cannot be blank")
    @Size(max = 255, message = "Receiver address cannot exceed 255 characters")
    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business methods
    public boolean isPendingPayment() {
        return STATUS_PENDING_PAYMENT.equals(this.status);
    }

    public boolean isPaid() {
        return STATUS_PAID.equals(this.status);
    }

    public boolean isShipped() {
        return STATUS_SHIPPED.equals(this.status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(this.status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(this.status);
    }

    public boolean canBeCancelled() {
        return isPendingPayment() || isPaid();
    }

    public boolean canBeShipped() {
        return isPaid();
    }

    public boolean canBeCompleted() {
        return isShipped();
    }

    public String getStatusText() {
        switch (this.status) {
            case 0: return "Pending Payment";
            case 1: return "Paid";
            case 2: return "Shipped";
            case 3: return "Completed";
            case 4: return "Cancelled";
            default: return "Unknown";
        }
    }
}
