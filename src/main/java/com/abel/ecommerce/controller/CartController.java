package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.dto.response.CartItemResponse;
import com.abel.ecommerce.dto.response.CartResponse;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.exception.CartItemNotFoundException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.service.CartService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart Management")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Add product to cart")
    @PostMapping("/add")
    public ResponseEntity<ResponseResult<CartItemResponse>> addToCart(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item data") @Valid @RequestBody CartItemRequest request) {
        try {
            CartItem cartItem = cartService.addToCart(userId, request);
            CartItemResponse response = new CartItemResponse();
            BeanUtils.copyProperties(cartItem, response);
            return ResponseEntity.ok(ResponseResult.ok(response));
        } catch (ProductNotFoundException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), "Product not found"));
        } catch (InsufficientStockException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Update cart item quantity")
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<ResponseResult<CartItemResponse>> updateCartItem(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId,
            @Parameter(description = "Updated cart item data") @Valid @RequestBody CartItemRequest request) {
        try {
            CartItem cartItem = cartService.updateCartItem(userId, cartItemId, request);
            CartItemResponse response = new CartItemResponse();
            BeanUtils.copyProperties(cartItem, response);
            return ResponseEntity.ok(ResponseResult.ok(response));
        } catch (CartItemNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InsufficientStockException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<ResponseResult<String>> removeFromCart(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId) {
        try {
            cartService.removeFromCart(userId, cartItemId);
            return ResponseEntity.ok(ResponseResult.ok("Item removed from cart successfully"));
        } catch (CartItemNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Get user cart")
    @GetMapping
    public ResponseEntity<ResponseResult<CartResponse>> getCart(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            CartResponse cartResponse = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(ResponseResult.ok(cartResponse));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Clear user cart")
    @DeleteMapping("/clear")
    public ResponseEntity<ResponseResult<String>> clearCart(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(ResponseResult.ok("Cart cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Update item selection status")
    @PutMapping("/select/{cartItemId}")
    public ResponseEntity<ResponseResult<String>> updateItemSelection(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId,
            @Parameter(description = "Selection status (0 or 1)") @RequestParam Integer selected) {
        try {
            cartService.updateItemSelection(userId, cartItemId, selected);
            return ResponseEntity.ok(ResponseResult.ok("Selection status updated successfully"));
        } catch (CartItemNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }
}
