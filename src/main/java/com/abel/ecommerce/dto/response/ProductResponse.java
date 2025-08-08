package com.abel.ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private String mainImage;
    private String images;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
