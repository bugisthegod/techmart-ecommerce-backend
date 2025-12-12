package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.CartItemNotFoundException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.service.CartService;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for CartController
 */
@EnableMethodSecurity
@WebMvcTest(CartController.class)
@DisplayName("CartController Web Layer Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Product testProduct;
    private CartItem testCartItem;
    private CartItemRequest testCartItemRequest;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Create test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setMainImage("test-image.jpg");
        testProduct.setStock(100);

        // Create test cart item
        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUserId(testUserId);
        testCartItem.setProductId(1L);
        testCartItem.setQuantity(5);
        testCartItem.setSelected(1);

        // Create test cart item request
        testCartItemRequest = new CartItemRequest();
        testCartItemRequest.setProductId(1L);
        testCartItemRequest.setQuantity(10);
        testCartItemRequest.setSelected(1);
    }

    // ========== ADD TO CART TESTS ==========

    @Test
    @DisplayName("Should add product to cart successfully")
    @WithMockUser
    void addToCart_Success() throws Exception {
        // Arrange
        when(cartService.addToCart(eq(testUserId), any(CartItemRequest.class))).thenReturn(testCartItem);
        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList(testCartItem));
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCartItemRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(cartService, times(1)).addToCart(eq(testUserId), any(CartItemRequest.class));
        verify(cartService, times(1)).getCartItemsByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return 400 when product ID is null")
    @WithMockUser
    void addToCart_NullProductId_BadRequest() throws Exception {
        // Arrange
        testCartItemRequest.setProductId(null);

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCartItemRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(anyLong(), any(CartItemRequest.class));
    }

    @Test
    @DisplayName("Should return error when insufficient stock")
    @WithMockUser
    void addToCart_InsufficientStock_ThrowsException() throws Exception {
        // Arrange
        when(cartService.addToCart(eq(testUserId), any(CartItemRequest.class)))
                .thenThrow(new InsufficientStockException("Test Product", 50, 100));

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCartItemRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE CART ITEM TESTS ==========

    @Test
    @DisplayName("Should update cart item quantity successfully")
    @WithMockUser
    void updateCartItem_Success() throws Exception {
        // Arrange
        when(cartService.updateCartItem(testUserId, 1L, 20)).thenReturn(testCartItem);
        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList(testCartItem));
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(put("/api/cart/update/{cartItemId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .param("quantity", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(testUserId));

        verify(cartService, times(1)).updateCartItem(testUserId, 1L, 20);
    }

    @Test
    @DisplayName("Should return error when updating non-existent cart item")
    @WithMockUser
    void updateCartItem_NotFound() throws Exception {
        // Arrange
        when(cartService.updateCartItem(testUserId, 999L, 20))
                .thenThrow(new CartItemNotFoundException(999L, "cart item ID"));

        // Act & Assert
        mockMvc.perform(put("/api/cart/update/{cartItemId}", 999L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .param("quantity", "20"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return error when updating with insufficient stock")
    @WithMockUser
    void updateCartItem_InsufficientStock() throws Exception {
        // Arrange
        when(cartService.updateCartItem(testUserId, 1L, 150))
                .thenThrow(new InsufficientStockException("Test Product", 100, 150));

        // Act & Assert
        mockMvc.perform(put("/api/cart/update/{cartItemId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .param("quantity", "150"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== REMOVE FROM CART TESTS ==========

    @Test
    @DisplayName("Should remove item from cart successfully")
    @WithMockUser
    void removeFromCart_Success() throws Exception {
        // Arrange
        doNothing().when(cartService).removeFromCart(testUserId, 1L);
        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList());
        when(productService.findProductById(anyLong())).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/remove/{cartItemId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(cartService, times(1)).removeFromCart(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when removing non-existent cart item")
    @WithMockUser
    void removeFromCart_NotFound() throws Exception {
        // Arrange
        doThrow(new CartItemNotFoundException(999L, "cart item ID"))
                .when(cartService).removeFromCart(testUserId, 999L);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/remove/{cartItemId}", 999L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== GET CART TESTS ==========

    @Test
    @DisplayName("Should get user cart successfully")
    @WithMockUser
    void getCart_Success() throws Exception {
        // Arrange
        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setUserId(testUserId);
        item2.setProductId(2L);
        item2.setQuantity(3);
        item2.setSelected(1);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("49.99"));
        product2.setMainImage("image2.jpg");

        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList(testCartItem, item2));
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(productService.findProductById(2L)).thenReturn(product2);

        // Act & Assert
        mockMvc.perform(get("/api/cart")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(2)));

        verify(cartService, times(1)).getCartItemsByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return empty cart when user has no items")
    @WithMockUser
    void getCart_EmptyCart() throws Exception {
        // Arrange
        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/cart")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    // ========== CLEAR CART TESTS ==========

    @Test
    @DisplayName("Should clear cart successfully")
    @WithMockUser
    void clearCart_Success() throws Exception {
        // Arrange
        doNothing().when(cartService).clearCart(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/clear")
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("Cart cleared successfully"));

        verify(cartService, times(1)).clearCart(testUserId);
    }

    // ========== UPDATE ITEM SELECTION TESTS ==========

    @Test
    @DisplayName("Should update item selection successfully")
    @WithMockUser
    void updateItemSelection_Success() throws Exception {
        // Arrange
        doNothing().when(cartService).updateItemSelection(testUserId, 1L, 0);

        // Act & Assert
        mockMvc.perform(put("/api/cart/select/{cartItemId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .param("selected", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("Selection status updated successfully"));

        verify(cartService, times(1)).updateItemSelection(testUserId, 1L, 0);
    }

    @Test
    @DisplayName("Should return error when updating selection for non-existent item")
    @WithMockUser
    void updateItemSelection_NotFound() throws Exception {
        // Arrange
        doThrow(new CartItemNotFoundException(999L, "cart item ID"))
                .when(cartService).updateItemSelection(testUserId, 999L, 0);

        // Act & Assert
        mockMvc.perform(put("/api/cart/select/{cartItemId}", 999L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .param("selected", "0"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate required fields in request")
    @WithMockUser
    void addToCart_InvalidRequest_ValidationError() throws Exception {
        // Arrange
        CartItemRequest invalidRequest = new CartItemRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(anyLong(), any(CartItemRequest.class));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser
    void addToCart_ServiceException_ErrorResponse() throws Exception {
        // Arrange
        when(cartService.addToCart(eq(testUserId), any(CartItemRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCartItemRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    // ========== CART CALCULATION TESTS ==========

    @Test
    @DisplayName("Should calculate cart totals correctly")
    @WithMockUser
    void getCart_CalculatesTotals() throws Exception {
        // Arrange
        testCartItem.setQuantity(2);
        testCartItem.setSelected(1);

        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setUserId(testUserId);
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setSelected(0); // Not selected

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("50.00"));
        product2.setMainImage("image2.jpg");

        when(cartService.getCartItemsByUserId(testUserId)).thenReturn(Arrays.asList(testCartItem, item2));
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(productService.findProductById(2L)).thenReturn(product2);

        // Act & Assert
        mockMvc.perform(get("/api/cart")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.selectedCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.selectedAmount").exists());
    }
}