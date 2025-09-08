package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Data
public class Category {

    public static final Integer ACTIVE_CATEGORY = 1;
    public static final Integer NONACTIVE_CATEGORY = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = ACTIVE_CATEGORY;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = 1;
        if (sortOrder == null) sortOrder = 0;
        if (parentId == null) parentId = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
