package com.abel.ecommerce.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryResponse {

    private Long id;

    private String name;

    private Long parentId;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long subcategoryCount;
}
