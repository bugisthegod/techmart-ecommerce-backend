package com.abel.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Category creation/update request")
public class CategoryRequest {

    @Schema(description = "Category name", example = "Electronics")
    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 50, message = "Category name cannot exceed 50 characters")
    private String name;

    @Schema(description = "Parent category ID (0 for top level)", example = "0")
    private Long parentId = 0L;

    @Schema(description = "Category icon URL", example = "https://example.com/icon.png")
    private String icon;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder = 0;

    @Schema(description = "Category status (0-hidden, 1-visible)", example = "1")
    private Integer status = 1;
}
