package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.entity.CartItem;

import java.util.List;

public interface CartService {

    /**
     * Add product to cart
     * @param userId User ID
     * @param request Cart item request
     * @return Created or updated cart item
     */
    CartItem addToCart(Long userId, CartItemRequest request);

    /**
     * Update cart item quantity
     * @param userId User ID
     * @param cartItemId Cart item ID
     * @param quantity New quantity
     * @return Updated cart item
     */
    CartItem updateCartItem(Long userId, Long cartItemId, Integer quantity);

    /**
     * Remove item from cart
     * @param userId User ID
     * @param cartItemId Cart item ID
     */
    void removeFromCart(Long userId, Long cartItemId);

    /**
     * Clear all items from cart
     * @param userId User ID
     */
    void clearCart(Long userId);

    /**
     * Update item selection status
     * @param userId User ID
     * @param cartItemId Cart item ID
     * @param selected Selection status (1 = selected, 0 = not selected)
     */
    void updateItemSelection(Long userId, Long cartItemId, Integer selected);

    /**
     * Get all cart items for a user
     * @param userId User ID
     * @return List of cart items
     */
    List<CartItem> getCartItemsByUserId(Long userId);

    /**
     * Get selected cart items for a user
     * @param userId User ID
     * @return List of selected cart items
     */
    List<CartItem> getSelectedCartItems(Long userId);

    /**
     * Find cart item by ID
     * @param cartItemId Cart item ID
     * @return Cart item entity
     */
    CartItem findCartItemById(Long cartItemId);
}
