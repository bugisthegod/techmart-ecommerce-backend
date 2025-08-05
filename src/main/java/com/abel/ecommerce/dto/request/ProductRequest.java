package com.abel.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Product request")
public class ProductRequest {

    @Schema(description = "Product name", example = "Donut Peach")
    @NotBlank(message = "Product name cannot be empty")
    private String name;

    @Schema(description = "Category ID", example = "9527")
    @NotBlank(message = "Category ID cannot be empty")
    private Long categoryId;

    @Schema(description = "Product price", example = "20.00")
    @NotBlank(message = "Product price cannot by empty")
    private BigDecimal price;

    private BigDecimal originalPrice;

    private String description;

    @Schema(description = "Main product image")
    @NotBlank(message = "Main product image cannot be empty")
    private String mainImage;

    private String images;

    private Integer stock;

    private Integer status;

}
