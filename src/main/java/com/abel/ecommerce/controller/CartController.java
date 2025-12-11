package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.dto.response.CartItemResponse;
import com.abel.ecommerce.dto.response.CartResponse;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.service.CartService;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.utils.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shopping Cart Management")
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    @Operation(summary = "Add product to cart")
    @PostMapping("/add")
    public ResponseResult<CartResponse> addToCart(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item data") @Valid @RequestBody CartItemRequest request) {
        cartService.addToCart(userId, request);
        List<CartItem> cartItems = cartService.getCartItemsByUserId(userId);
        CartResponse response = convertToCartResponse(userId, cartItems);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Update cart item quantity")
    @PutMapping("/update/{cartItemId}")
    public ResponseResult<CartResponse> updateCartItem(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId,
            @Parameter(description = "Cart item quantity") @RequestParam Integer quantity) {
        cartService.updateCartItem(userId, cartItemId, quantity);
        List<CartItem> cartItems = cartService.getCartItemsByUserId(userId);
        CartResponse response = convertToCartResponse(userId, cartItems);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseResult<CartResponse> removeFromCart(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId) {
        cartService.removeFromCart(userId, cartItemId);
        List<CartItem> cartItems = cartService.getCartItemsByUserId(userId);
        CartResponse response = convertToCartResponse(userId, cartItems);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Get user cart")
    @GetMapping
    public ResponseResult<CartResponse> getCart(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        List<CartItem> cartItems = cartService.getCartItemsByUserId(userId);
        CartResponse cartResponse = convertToCartResponse(userId, cartItems);
        return ResponseResult.ok(cartResponse);
    }

    @Operation(summary = "Clear user cart")
    @DeleteMapping("/clear")
    public ResponseResult<String> clearCart(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseResult.ok("Cart cleared successfully");
    }

    @Operation(summary = "Update item selection status")
    @PutMapping("/select/{cartItemId}")
    public ResponseResult<String> updateItemSelection(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId,
            @Parameter(description = "Selection status (0 or 1)") @RequestParam Integer selected) {
        cartService.updateItemSelection(userId, cartItemId, selected);
        return ResponseResult.ok("Selection status updated successfully");
    }

    /**
     * Convert CartItems to CartResponse
     */
    private CartResponse convertToCartResponse(Long userId, List<CartItem> cartItems) {
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        CartResponse cartResponse = new CartResponse();
        cartResponse.setUserId(userId);
        cartResponse.setItems(itemResponses);
        cartResponse.setTotalItems(itemResponses.size());

        // Calculate totals
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal selectedAmount = itemResponses.stream()
                .filter(item -> item.getSelected() == 1)
                .map(CartItemResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int selectedCount = (int) itemResponses.stream()
                .filter(item -> item.getSelected() == 1)
                .count();

        cartResponse.setTotalAmount(totalAmount);
        cartResponse.setSelectedAmount(selectedAmount);
        cartResponse.setSelectedCount(selectedCount);

        return cartResponse;
    }

    /**
     * Convert CartItem entity to CartItemResponse DTO
     */
    private CartItemResponse convertToCartItemResponse(CartItem cartItem) {
        CartItemResponse response = new CartItemResponse();
        BeanUtils.copyProperties(cartItem, response);

        // Get product details
        try {
            Product product = productService.findProductById(cartItem.getProductId());
            response.setProductName(product.getName());
            response.setProductImage(product.getMainImage());
            response.setProductPrice(product.getPrice());
            response.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        catch (ProductNotFoundException e) {
            // Handle case where product might have been deleted
            response.setProductName("Product not found");
            response.setProductPrice(BigDecimal.ZERO);
            response.setTotalAmount(BigDecimal.ZERO);
        }

        return response;
    }
}
