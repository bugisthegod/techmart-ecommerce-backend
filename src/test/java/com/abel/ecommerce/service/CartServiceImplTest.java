package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.CartItemRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.CartItemNotFoundException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.repository.CartItemRepository;
import com.abel.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CartServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartServiceImpl cartService;

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
        testProduct.setStock(100);
        testProduct.setStatus(Product.ACTIVE_PRODUCT);

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
    @DisplayName("Should add new item to cart successfully")
    void addToCart_NewItem_Success() {
        // Arrange
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.existsByUserIdAndProductId(testUserId, 1L)).thenReturn(false);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        CartItem result = cartService.addToCart(testUserId, testCartItemRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getSelected()).isEqualTo(1);

        verify(productService, times(1)).findProductById(1L);
        verify(cartItemRepository, times(1)).existsByUserIdAndProductId(testUserId, 1L);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should update existing cart item when adding same product")
    void addToCart_ExistingItem_UpdateQuantity() {
        // Arrange
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.existsByUserIdAndProductId(testUserId, 1L)).thenReturn(true);
        when(cartItemRepository.findByUserIdAndProductId(testUserId, 1L))
                .thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartItem result = cartService.addToCart(testUserId, testCartItemRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(15); // 5 existing + 10 new
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when adding quantity exceeds stock")
    void addToCart_InsufficientStock_ThrowsException() {
        // Arrange
        testCartItemRequest.setQuantity(150); // More than available stock (100)
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(testUserId, testCartItemRequest))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when existing + new quantity exceeds stock")
    void addToCart_ExistingItemInsufficientStock_ThrowsException() {
        // Arrange
        testCartItem.setQuantity(95); // Existing quantity
        testCartItemRequest.setQuantity(10); // New quantity, total = 105 > stock 100
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.existsByUserIdAndProductId(testUserId, 1L)).thenReturn(true);
        when(cartItemRepository.findByUserIdAndProductId(testUserId, 1L))
                .thenReturn(Optional.of(testCartItem));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(testUserId, testCartItemRequest))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // ========== UPDATE CART ITEM TESTS ==========

    @Test
    @DisplayName("Should update cart item quantity successfully")
    void updateCartItem_Success() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartItem result = cartService.updateCartItem(testUserId, 1L, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(20);
        assertThat(result.getSelected()).isEqualTo(1); // Should be selected by default
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating with quantity exceeding stock")
    void updateCartItem_InsufficientStock_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateCartItem(testUserId, 1L, 150))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating cart item not belonging to user")
    void updateCartItem_NotBelongToUser_ThrowsException() {
        // Arrange
        testCartItem.setUserId(999L); // Different user
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateCartItem(testUserId, 1L, 10))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("does not belong to user");

        verify(productService, never()).findProductById(anyLong());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent cart item")
    void updateCartItem_NotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateCartItem(testUserId, 999L, 10))
                .isInstanceOf(CartItemNotFoundException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // ========== REMOVE FROM CART TESTS ==========

    @Test
    @DisplayName("Should remove cart item successfully")
    void removeFromCart_Success() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        doNothing().when(cartItemRepository).deleteById(1L);

        // Act
        cartService.removeFromCart(testUserId, 1L);

        // Assert
        verify(cartItemRepository, times(1)).findById(1L);
        verify(cartItemRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when removing cart item not belonging to user")
    void removeFromCart_NotBelongToUser_ThrowsException() {
        // Arrange
        testCartItem.setUserId(999L);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));

        // Act & Assert
        assertThatThrownBy(() -> cartService.removeFromCart(testUserId, 1L))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("does not belong to user");

        verify(cartItemRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent cart item")
    void removeFromCart_NotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.removeFromCart(testUserId, 999L))
                .isInstanceOf(CartItemNotFoundException.class);

        verify(cartItemRepository, never()).deleteById(anyLong());
    }

    // ========== CLEAR CART TESTS ==========

    @Test
    @DisplayName("Should clear all cart items for user")
    void clearCart_Success() {
        // Arrange
        doNothing().when(cartItemRepository).deleteByUserId(testUserId);

        // Act
        cartService.clearCart(testUserId);

        // Assert
        verify(cartItemRepository, times(1)).deleteByUserId(testUserId);
    }

    // ========== UPDATE ITEM SELECTION TESTS ==========

    @Test
    @DisplayName("Should update item selection successfully")
    void updateItemSelection_Success() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cartService.updateItemSelection(testUserId, 1L, 0);

        // Assert
        verify(cartItemRepository, times(1)).findById(1L);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating selection for item not belonging to user")
    void updateItemSelection_NotBelongToUser_ThrowsException() {
        // Arrange
        testCartItem.setUserId(999L);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateItemSelection(testUserId, 1L, 0))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("does not belong to user");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating selection for non-existent item")
    void updateItemSelection_NotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateItemSelection(testUserId, 999L, 0))
                .isInstanceOf(CartItemNotFoundException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // ========== GET CART ITEMS TESTS ==========

    @Test
    @DisplayName("Should get all cart items for user")
    void getCartItemsByUserId_Success() {
        // Arrange
        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setUserId(testUserId);
        item2.setProductId(2L);
        item2.setQuantity(3);

        List<CartItem> cartItems = Arrays.asList(testCartItem, item2);
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId)).thenReturn(cartItems);

        // Act
        List<CartItem> result = cartService.getCartItemsByUserId(testUserId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
        verify(cartItemRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    @Test
    @DisplayName("Should return empty list when user has no cart items")
    void getCartItemsByUserId_EmptyCart() {
        // Arrange
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId)).thenReturn(Arrays.asList());

        // Act
        List<CartItem> result = cartService.getCartItemsByUserId(testUserId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get only selected cart items")
    void getSelectedCartItems_Success() {
        // Arrange
        CartItem selectedItem = new CartItem();
        selectedItem.setId(2L);
        selectedItem.setUserId(testUserId);
        selectedItem.setSelected(1);

        List<CartItem> selectedItems = Arrays.asList(testCartItem, selectedItem);
        when(cartItemRepository.findByUserIdAndSelected(testUserId, 1)).thenReturn(selectedItems);

        // Act
        List<CartItem> result = cartService.getSelectedCartItems(testUserId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(item -> item.getSelected() == 1);
        verify(cartItemRepository, times(1)).findByUserIdAndSelected(testUserId, 1);
    }

    @Test
    @DisplayName("Should return empty list when no items are selected")
    void getSelectedCartItems_NoneSelected() {
        // Arrange
        when(cartItemRepository.findByUserIdAndSelected(testUserId, 1)).thenReturn(Arrays.asList());

        // Act
        List<CartItem> result = cartService.getSelectedCartItems(testUserId);

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== FIND CART ITEM BY ID TESTS ==========

    @Test
    @DisplayName("Should find cart item by ID successfully")
    void findCartItemById_Success() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));

        // Act
        CartItem result = cartService.findCartItemById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(cartItemRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when cart item not found by ID")
    void findCartItemById_NotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.findCartItemById(999L))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should add exact stock quantity to new cart item")
    void addToCart_ExactStock_Success() {
        // Arrange
        testCartItemRequest.setQuantity(100); // Exact stock
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.existsByUserIdAndProductId(testUserId, 1L)).thenReturn(false);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartItem result = cartService.addToCart(testUserId, testCartItemRequest);

        // Assert
        assertThat(result.getQuantity()).isEqualTo(100);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should update to exact stock quantity")
    void updateCartItem_ExactStock_Success() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        when(productService.findProductById(1L)).thenReturn(testProduct);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartItem result = cartService.updateCartItem(testUserId, 1L, 100);

        // Assert
        assertThat(result.getQuantity()).isEqualTo(100);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }
}