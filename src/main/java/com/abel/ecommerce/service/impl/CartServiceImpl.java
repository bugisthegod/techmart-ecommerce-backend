package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.CartItemNotFoundException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.repository.CartItemRepository;
import com.abel.ecommerce.service.CartService;
import com.abel.ecommerce.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public CartItem addToCart(Long userId, CartItemRequest request) {
        // Check if product exists && Check  if stock is enough
        Product productById = productService.findProductById(request.getProductId());
        if (request.getQuantity() > productById.getStock()) throw new InsufficientStockException(productById.getName(), productById.getStock(),
                request.getSelected());

        // Check if cart item has already existed
        // If cart item exists
        if (cartItemRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            CartItem cartItemExists = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId()).get();
            int newQuantity = cartItemExists.getQuantity() + request.getQuantity();
            // Check if newQuantity over stock
            if (newQuantity > productById.getStock()) throw new InsufficientStockException(productById.getName(), productById.getStock(),
                    newQuantity);
            cartItemExists.setQuantity(newQuantity);
            return cartItemRepository.save(cartItemExists);
        }

        // If cart item does not exist
        CartItem newCartItem = new CartItem();
        newCartItem.setQuantity(request.getQuantity());
        newCartItem.setUserId(userId);
        newCartItem.setProductId(request.getProductId());
        newCartItem.setSelected(1); // Selected
        return cartItemRepository.save(newCartItem);
    }

    @Override
    @Transactional
    public CartItem updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        // Check if cart existed
        CartItem cartItemById = findCartItemById(cartItemId);

        // Check if cart item belongs to this user
        if (!cartItemById.getUserId().equals(userId)) throw new CartItemNotFoundException("Cart Item does not belong to user");

        // Get product using existing productId from cartItem
        Product productById = productService.findProductById(cartItemById.getProductId());

        // Check if new quantity exceeds stock
        if (quantity > productById.getStock())
            throw new InsufficientStockException(productById.getName(), productById.getStock(), quantity);

        // Update cart Item quantity and set as selected by default
        cartItemById.setQuantity(quantity);
        cartItemById.setSelected(1); // Default selected
        return cartItemRepository.save(cartItemById);
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        // Check if cart Item existed
        CartItem cartItemById = findCartItemById(cartItemId);

        // Check if cart Item belongs to user
        if (!cartItemById.getUserId().equals(userId)) throw new CartItemNotFoundException("Cart Item does not belong to user");

        // Delete cart Item
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void updateItemSelection(Long userId, Long cartItemId, Integer selected) {
        // Check if cart Item existed
        CartItem cartItemById = findCartItemById(cartItemId);

        // Check if cart Item belongs to user
        if (!cartItemById.getUserId().equals(userId)) throw new CartItemNotFoundException("Cart Item does not belong to user");

        // Update cart Item selected
        cartItemById.setSelected(selected);
        cartItemRepository.save(cartItemById);
    }

    @Override
    public List<CartItem> getCartItemsByUserId(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<CartItem> getSelectedCartItems(Long userId) {
        return cartItemRepository.findByUserIdAndSelected(userId, 1);
    }

    @Override
    public CartItem findCartItemById(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId, "cart Item ID"));
    }

}
