package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Find cart items by user ID
    List<CartItem> findByUserId(Long userId);

    // Find cart items by user ID and selected status
    List<CartItem> findByUserIdAndSelected(Long userId, Integer selected);

    // Find specific cart item by user and product
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    // Find cart items by user ID order by createdAt desc
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Check if cart item exists
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Delete cart items by user ID
    void deleteByUserId(Long userId);

    // Delete cart items by user ID and product IDs
    void deleteByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    // Count cart items by user
    long countByUserId(Long userId);

    // Find selected cart items by user
    List<CartItem> findByUserIdAndSelectedOrderByCreatedAtDesc(Long userId, Integer selected);
}
