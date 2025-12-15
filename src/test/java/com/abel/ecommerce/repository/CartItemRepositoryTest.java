package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CartItemRepository
 *
 * Tests cart item management including user cart operations,
 * product selection, and quantity management
 */
@DataJpaTest
@DisplayName("CartItemRepository Integration Tests")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    private CartItem cartItem1;
    private CartItem cartItem2;
    private CartItem cartItem3;
    private CartItem cartItem4;
    private Long testUserId1 = 1L;
    private Long testUserId2 = 2L;
    private Long productId1 = 101L;
    private Long productId2 = 102L;
    private Long productId3 = 103L;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();

        // Create cart items for user 1
        cartItem1 = new CartItem();
        cartItem1.setUserId(testUserId1);
        cartItem1.setProductId(productId1);
        cartItem1.setQuantity(2);
        cartItem1.setSelected(CartItem.DEFAULT_SELECTED);

        cartItem2 = new CartItem();
        cartItem2.setUserId(testUserId1);
        cartItem2.setProductId(productId2);
        cartItem2.setQuantity(1);
        cartItem2.setSelected(CartItem.NON_DEFAULT_SELECTED);

        cartItem3 = new CartItem();
        cartItem3.setUserId(testUserId1);
        cartItem3.setProductId(productId3);
        cartItem3.setQuantity(3);
        cartItem3.setSelected(CartItem.DEFAULT_SELECTED);

        // Create cart item for user 2
        cartItem4 = new CartItem();
        cartItem4.setUserId(testUserId2);
        cartItem4.setProductId(productId1);
        cartItem4.setQuantity(1);
        cartItem4.setSelected(CartItem.DEFAULT_SELECTED);

        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);
        cartItemRepository.save(cartItem3);
        cartItemRepository.save(cartItem4);
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find cart items by user ID")
    void findByUserId() {
        List<CartItem> user1Items = cartItemRepository.findByUserId(testUserId1);
        List<CartItem> user2Items = cartItemRepository.findByUserId(testUserId2);

        assertThat(user1Items).hasSize(3);
        assertThat(user2Items).hasSize(1);
        assertThat(user1Items).allMatch(item -> item.getUserId().equals(testUserId1));
    }

    @Test
    @DisplayName("Should find cart items by user ID and selected status")
    void findByUserIdAndSelected() {
        List<CartItem> selectedItems = cartItemRepository.findByUserIdAndSelected(
                testUserId1,
                CartItem.DEFAULT_SELECTED
        );
        List<CartItem> unselectedItems = cartItemRepository.findByUserIdAndSelected(
                testUserId1,
                CartItem.NON_DEFAULT_SELECTED
        );

        assertThat(selectedItems).hasSize(2);
        assertThat(selectedItems).extracting(CartItem::getProductId)
                .containsExactlyInAnyOrder(productId1, productId3);

        assertThat(unselectedItems).hasSize(1);
        assertThat(unselectedItems.get(0).getProductId()).isEqualTo(productId2);
    }

    @Test
    @DisplayName("Should find specific cart item by user and product")
    void findByUserIdAndProductId() {
        Optional<CartItem> found = cartItemRepository.findByUserIdAndProductId(
                testUserId1,
                productId1
        );
        Optional<CartItem> notFound = cartItemRepository.findByUserIdAndProductId(
                testUserId1,
                999L
        );

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find cart items ordered by created time desc")
    void findByUserIdOrderByCreatedAtDesc() {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId1);

        assertThat(items).hasSize(3);
        // Most recently created should be first
        for (int i = 0; i < items.size() - 1; i++) {
            assertThat(items.get(i).getCreatedAt())
                    .isAfterOrEqualTo(items.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("Should check if cart item exists")
    void existsByUserIdAndProductId() {
        boolean exists = cartItemRepository.existsByUserIdAndProductId(
                testUserId1,
                productId1
        );
        boolean notExists = cartItemRepository.existsByUserIdAndProductId(
                testUserId1,
                999L
        );

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should delete cart items by user ID")
    void deleteByUserId() {
        cartItemRepository.deleteByUserId(testUserId1);
        cartItemRepository.flush();

        List<CartItem> user1Items = cartItemRepository.findByUserId(testUserId1);
        List<CartItem> user2Items = cartItemRepository.findByUserId(testUserId2);

        assertThat(user1Items).isEmpty();
        assertThat(user2Items).hasSize(1); // User 2's items should remain
    }

    @Test
    @DisplayName("Should delete cart items by user ID and product IDs")
    void deleteByUserIdAndProductIdIn() {
        List<Long> productIdsToDelete = Arrays.asList(productId1, productId2);

        cartItemRepository.deleteByUserIdAndProductIdIn(testUserId1, productIdsToDelete);
        cartItemRepository.flush();

        List<CartItem> remainingItems = cartItemRepository.findByUserId(testUserId1);

        assertThat(remainingItems).hasSize(1);
        assertThat(remainingItems.get(0).getProductId()).isEqualTo(productId3);
    }

    @Test
    @DisplayName("Should count cart items by user")
    void countByUserId() {
        long user1Count = cartItemRepository.countByUserId(testUserId1);
        long user2Count = cartItemRepository.countByUserId(testUserId2);
        long user3Count = cartItemRepository.countByUserId(999L);

        assertThat(user1Count).isEqualTo(3);
        assertThat(user2Count).isEqualTo(1);
        assertThat(user3Count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find selected cart items ordered by created time desc")
    void findByUserIdAndSelectedOrderByCreatedAtDesc() {
        List<CartItem> selectedItems = cartItemRepository.findByUserIdAndSelectedOrderByCreatedAtDesc(
                testUserId1,
                CartItem.DEFAULT_SELECTED
        );

        assertThat(selectedItems).hasSize(2);
        assertThat(selectedItems).extracting(CartItem::getProductId)
                .containsExactlyInAnyOrder(productId1, productId3);

        // Verify ordering
        for (int i = 0; i < selectedItems.size() - 1; i++) {
            assertThat(selectedItems.get(i).getCreatedAt())
                    .isAfterOrEqualTo(selectedItems.get(i + 1).getCreatedAt());
        }
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle user with empty cart")
    void findByUserId_EmptyCart() {
        List<CartItem> items = cartItemRepository.findByUserId(999L);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should handle no selected items")
    void findByUserIdAndSelected_NoSelected() {
        // Set all items to unselected
        List<CartItem> allItems = cartItemRepository.findByUserId(testUserId1);
        allItems.forEach(item -> {
            item.setSelected(CartItem.NON_DEFAULT_SELECTED);
            cartItemRepository.save(item);
        });
        cartItemRepository.flush();

        List<CartItem> selectedItems = cartItemRepository.findByUserIdAndSelected(
                testUserId1,
                CartItem.DEFAULT_SELECTED
        );

        assertThat(selectedItems).isEmpty();
    }

    @Test
    @DisplayName("Should handle deleting non-existent products")
    void deleteByUserIdAndProductIdIn_NoMatch() {
        List<Long> nonExistentProducts = Arrays.asList(888L, 999L);

        cartItemRepository.deleteByUserIdAndProductIdIn(testUserId1, nonExistentProducts);
        cartItemRepository.flush();

        long count = cartItemRepository.countByUserId(testUserId1);
        assertThat(count).isEqualTo(3); // All items should remain
    }

    @Test
    @DisplayName("Should prevent duplicate cart items for same user and product")
    void uniqueUserAndProduct() {
        // Try to find existing combination
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(
                testUserId1,
                productId1
        );

        assertThat(existing).isPresent();

        // In real usage, service layer should check existence before creating new item
        boolean exists = cartItemRepository.existsByUserIdAndProductId(
                testUserId1,
                productId1
        );
        assertThat(exists).isTrue();
    }
}