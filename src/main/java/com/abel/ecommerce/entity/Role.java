package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ORDER_ADMIN = "ORDER_ADMIN";
    public static final String ROLE_PRODUCT_ADMIN = "PRODUCT_ADMIN";

    public static final Integer ACTIVE_ROLE = 1;
    public static final Integer NONACTIVE_ROLE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable =false)
    private String code;

    private String description;

    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = ACTIVE_ROLE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

}
