package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.repository.OrderItemRepository;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl
 * <p>
 * Key concepts demonstrated:
 * - @ExtendWith(MockitoExtension.class): Enables Mockito annotations
 * - @Mock: Creates mock objects (dependencies)
 * - @InjectMocks: Creates instance and injects mocks into it
 * - @BeforeEach: Runs before each test method
 * - @Test: Marks a test method
 * - @DisplayName: Provides readable test name
 * <p>
 * All core logic tests are implemented including:
 * - Pay order tests (success, exception handling)
 * - Cancel order tests (success, business rule validation)
 * - Generate order number tests (uniqueness, user suffix verification)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderServiceImpl orderService;

    private AutoCloseable closeable;

    private Order testOrder;
    private Order testPendingOrder;
    private Order testPaidOrder;
    private Order testShippedOrder;
    private Order testCompletedOrder;
    private Order testCancelledOrder;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;
    private List<OrderItem> testOrderItems;

    /**
     * Setup method - runs before each test
     * Creates common test data to avoid duplication
     */
    @BeforeEach
    void setUp() {

        // Create test order items
        testOrderItem1 = new OrderItem();
        testOrderItem1.setId(1L);
        testOrderItem1.setOrderId(1L);
        testOrderItem1.setOrderNo("20251229143022012345");
        testOrderItem1.setProductId(100L);
        testOrderItem1.setProductName("Test Product 1");
        testOrderItem1.setProductImage("product1.jpg");
        testOrderItem1.setProductPrice(new BigDecimal("99.99"));
        testOrderItem1.setQuantity(2);

        testOrderItem2 = new OrderItem();
        testOrderItem2.setId(2L);
        testOrderItem2.setOrderId(1L);
        testOrderItem2.setOrderNo("20251229143022012345");
        testOrderItem2.setProductId(101L);
        testOrderItem2.setProductName("Test Product 2");
        testOrderItem2.setProductImage("product2.jpg");
        testOrderItem2.setProductPrice(new BigDecimal("49.99"));
        testOrderItem2.setQuantity(1);

        testOrderItems = Arrays.asList(testOrderItem1, testOrderItem2);

        // Create test order in PENDING_PAYMENT status
        testPendingOrder = createOrderWithStatus(Order.STATUS_PENDING_PAYMENT);

        // Create test order in PAID status
        testPaidOrder = createOrderWithStatus(Order.STATUS_PAID);
        testPaidOrder.setPaymentTime(LocalDateTime.now().minusHours(1));

        // Create test order in SHIPPED status
        testShippedOrder = createOrderWithStatus(Order.STATUS_SHIPPED);
        testShippedOrder.setPaymentTime(LocalDateTime.now().minusDays(2));
        testShippedOrder.setDeliveryTime(LocalDateTime.now().minusDays(1));

        // Create test order in COMPLETED status
        testCompletedOrder = createOrderWithStatus(Order.STATUS_COMPLETED);
        testCompletedOrder.setPaymentTime(LocalDateTime.now().minusDays(5));
        testCompletedOrder.setDeliveryTime(LocalDateTime.now().minusDays(3));
        testCompletedOrder.setReceiveTime(LocalDateTime.now().minusDays(1));

        // Create test order in CANCELLED status
        testCancelledOrder = createOrderWithStatus(Order.STATUS_CANCELLED);

        // Default test order for generic tests
        testOrder = testPendingOrder;
    }

    /**
     * Helper method to create order with specific status
     */
    private Order createOrderWithStatus(Integer status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20251229143022012345");
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("259.97"));
        order.setPayAmount(new BigDecimal("259.97"));
        order.setFreightAmount(new BigDecimal("10.00"));
        order.setStatus(status);
        order.setReceiverName("John Doe");
        order.setReceiverPhone("1234567890");
        order.setReceiverAddress("123 Test Street, Test City");
        order.setComment("Test order comment");
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    // ========== PAY ORDER TESTS ==========

    @Test
    @DisplayName("Should pay order successfully when in pending payment status")
    void payOrder_Success() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testPendingOrder);

        // Act
        Order result = orderService.payOrder(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.STATUS_PAID);
        assertThat(result.getPaymentTime()).isNotNull();

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when paying already paid order")
    void payOrder_AlreadyPaid_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPaidOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.payOrder(1L, 1L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("not in pending payment status");

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when user doesn't own the order")
    void payOrder_UnauthorizedUser_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.payOrder(999L, 1L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found or doesn't belong to user");

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 999L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ========== SHIP ORDER TESTS ==========

    @Test
    @DisplayName("Should ship order successfully when in paid status")
    void shipOrder_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testPaidOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testPaidOrder);

        // Act
        Order result = orderService.shipOrder(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.STATUS_SHIPPED);
        assertThat(result.getDeliveryTime()).isNotNull();

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when shipping order not in paid status")
    void shipOrder_InvalidStatus_ThrowsException() {
        // Arrange - try to ship a pending payment order
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testPendingOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.shipOrder(1L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("paid order can be shipped");

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found for shipping")
    void shipOrder_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.shipOrder(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");

        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ========== COMPLETE ORDER TESTS ==========

    @Test
    @DisplayName("Should complete order successfully when in shipped status")
    void completeOrder_Success() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testShippedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testShippedOrder);

        // Act
        Order result = orderService.completeOrder(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.STATUS_COMPLETED);
        assertThat(result.getReceiveTime()).isNotNull();

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when completing order not in shipped status")
    void completeOrder_InvalidStatus_ThrowsException() {
        // Arrange - try to complete a paid (not shipped) order
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPaidOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.completeOrder(1L, 1L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("cannot be completed");

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found for completion")
    void completeOrder_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.completeOrder(1L, 999L))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, times(1)).findByIdAndUserId(999L, 1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ========== CANCEL ORDER TESTS ==========

    @Test
    @DisplayName("Should cancel order successfully when in pending payment status")
    void cancelOrder_PendingPayment_Success() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testPendingOrder);

        // Act
        Order result = orderService.cancelOrder(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.STATUS_CANCELLED);

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should cancel order successfully when in paid status")
    void cancelOrder_Paid_Success() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPaidOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testPaidOrder);

        // Act
        Order result = orderService.cancelOrder(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.STATUS_CANCELLED);

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling shipped order")
    void cancelOrder_Shipped_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testShippedOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ========== FIND ORDER TESTS ==========

    @Test
    @DisplayName("Should find order by ID successfully")
    void findOrderById_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.findOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderNo()).isEqualTo("20251229143022012345");

        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when order not found by ID")
    void findOrderById_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.findOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");

        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should find order by ID and user ID successfully")
    void findOrderByIdAndUserId_Success() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.findOrderByIdAndUserId(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Should throw exception when order not found for user")
    void findOrderByIdAndUserId_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.findOrderByIdAndUserId(1L, 2L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("not found or doesn't belong to user");

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 2L);
    }

    @Test
    @DisplayName("Should find orders by user ID with pagination")
    void findOrdersByUserId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);

        // Act
        Page<Order> result = orderService.findOrdersByUserId(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(1L);

        verify(orderRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    @DisplayName("Should find orders by user ID and status with pagination")
    void findOrdersByUserIdAndStatus_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testPaidOrder));
        when(orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, Order.STATUS_PAID, pageable))
                .thenReturn(orderPage);

        // Act
        Page<Order> result = orderService.findOrdersByUserIdAndStatus(1L, Order.STATUS_PAID, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(Order.STATUS_PAID);

        verify(orderRepository, times(1))
                .findByUserIdAndStatusOrderByCreatedAtDesc(1L, Order.STATUS_PAID, pageable);
    }

    @Test
    @DisplayName("Should find order by order number successfully")
    void findOrderByOrderNo_Success() {
        // Arrange
        String orderNo = "20251229143022012345";
        when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.findOrderByOrderNo(orderNo);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo(orderNo);

        verify(orderRepository, times(1)).findByOrderNo(orderNo);
    }

    @Test
    @DisplayName("Should throw exception when order not found by order number")
    void findOrderByOrderNo_NotFound_ThrowsException() {
        // Arrange
        String orderNo = "INVALID_ORDER_NO";
        when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.findOrderByOrderNo(orderNo))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderNo);

        verify(orderRepository, times(1)).findByOrderNo(orderNo);
    }

    // ========== ORDER ITEMS TESTS ==========

    @Test
    @DisplayName("Should find order items by order ID")
    void findOrderItems_Success() {
        // Arrange
        when(orderItemRepository.findByOrderId(1L)).thenReturn(testOrderItems);

        // Act
        List<OrderItem> result = orderService.findOrderItems(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(1).getOrderId()).isEqualTo(1L);

        verify(orderItemRepository, times(1)).findByOrderId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no order items found")
    void findOrderItems_EmptyList() {
        // Arrange
        when(orderItemRepository.findByOrderId(999L)).thenReturn(Arrays.asList());

        // Act
        List<OrderItem> result = orderService.findOrderItems(999L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(orderItemRepository, times(1)).findByOrderId(999L);
    }

    @Test
    @DisplayName("Should find order items by order number")
    void findOrderItemsByOrderNo_Success() {
        // Arrange
        String orderNo = "20251229143022012345";
        when(orderItemRepository.findByOrderNo(orderNo)).thenReturn(testOrderItems);

        // Act
        List<OrderItem> result = orderService.findOrderItemsByOrderNo(orderNo);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderNo()).isEqualTo(orderNo);

        verify(orderItemRepository, times(1)).findByOrderNo(orderNo);
    }

    // ========== COUNT TESTS ==========

    @Test
    @DisplayName("Should count orders by user ID")
    void countOrdersByUserId_Success() {
        // Arrange
        when(orderRepository.countByUserId(1L)).thenReturn(5L);

        // Act
        long result = orderService.countOrdersByUserId(1L);

        // Assert
        assertThat(result).isEqualTo(5L);

        verify(orderRepository, times(1)).countByUserId(1L);
    }

    @Test
    @DisplayName("Should count orders by user ID and status")
    void countOrdersByUserIdAndStatus_Success() {
        // Arrange
        when(orderRepository.countByUserIdAndStatus(1L, Order.STATUS_PAID)).thenReturn(3L);

        // Act
        long result = orderService.countOrdersByUserIdAndStatus(1L, Order.STATUS_PAID);

        // Assert
        assertThat(result).isEqualTo(3L);

        verify(orderRepository, times(1)).countByUserIdAndStatus(1L, Order.STATUS_PAID);
    }

    // ========== GENERATE ORDER NUMBER TESTS ==========

    @Test
    @DisplayName("Should generate unique order number with correct format")
    void generateOrderNo_ReturnsUniqueFormat() throws InterruptedException {
        // Act - generate multiple order numbers
        List<String> orderNumbers = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String orderNo = orderService.generateOrderNo(1L);
            orderNumbers.add(orderNo);
            Thread.sleep(10); // Small delay to ensure timestamp difference
        }

        // Assert - check each order number
        for (String orderNo : orderNumbers) {
            assertThat(orderNo).isNotNull();
            assertThat(orderNo).hasSize(21);
            assertThat(orderNo).matches("\\d{21}"); // All digits

            // Extract and verify user suffix (characters 14-17)
            String userSuffix = orderNo.substring(14, 18);
            assertThat(userSuffix).isEqualTo("0001"); // userId 1 % 10000 = 1, formatted as "0001"
        }

        // Assert uniqueness - all order numbers should be different
        assertThat(orderNumbers).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should generate different order numbers for different users")
    void generateOrderNo_DifferentUsers_DifferentSuffixes() {
        // Act
        String orderNo1 = orderService.generateOrderNo(1L);
        String orderNo2 = orderService.generateOrderNo(12345L);

        // Assert - both order numbers are valid
        assertThat(orderNo1).isNotNull();
        assertThat(orderNo1).hasSize(21);
        assertThat(orderNo2).isNotNull();
        assertThat(orderNo2).hasSize(21);

        // Extract user suffixes (characters 14-17)
        String userSuffix1 = orderNo1.substring(14, 18);
        String userSuffix2 = orderNo2.substring(14, 18);

        // Assert - verify expected suffixes
        assertThat(userSuffix1).isEqualTo("0001"); // 1 % 10000 = 1 -> "0001"
        assertThat(userSuffix2).isEqualTo("2345"); // 12345 % 10000 = 2345 -> "2345"

        // Assert - suffixes are different
        assertThat(userSuffix1).isNotEqualTo(userSuffix2);
    }

    // ========== ORDER TOKEN TESTS ==========

    @Test
    @DisplayName("Should generate order token and store in Redis")
    void generateOrderToken_Success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        // Act
        String token = orderService.generateOrderToken(1L);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).startsWith("ORDER_TOKEN_");
        assertThat(token).contains("_1_"); // Contains user ID

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should validate and delete order token successfully")
    void validateAndDeleteOrderToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = "ORDER_TOKEN_1735468800000_1_abcd1234";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        boolean result = orderService.validateAndDeleteOrderToken(token);

        // Assert
        assertThat(result).isTrue();

        verify(redisTemplate, times(1)).delete("order:token:" + token);
    }

    @Test
    @DisplayName("Should return false when token doesn't exist or already used")
    void validateAndDeleteOrderToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String token = "INVALID_TOKEN";
        when(redisTemplate.delete(anyString())).thenReturn(false);

        // Act
        boolean result = orderService.validateAndDeleteOrderToken(token);

        // Assert
        assertThat(result).isFalse();

        verify(redisTemplate, times(1)).delete("order:token:" + token);
    }

    // ========== EXISTS TESTS ==========

    @Test
    @DisplayName("Should return true when order number exists")
    void existsByOrderNo_Exists_ReturnsTrue() {
        // Arrange
        String orderNo = "20251229143022012345";
        when(orderRepository.existsByOrderNo(orderNo)).thenReturn(true);

        // Act
        boolean result = orderService.existsByOrderNo(orderNo);

        // Assert
        assertThat(result).isTrue();

        verify(orderRepository, times(1)).existsByOrderNo(orderNo);
    }

    @Test
    @DisplayName("Should return false when order number doesn't exist")
    void existsByOrderNo_NotExists_ReturnsFalse() {
        // Arrange
        String orderNo = "NONEXISTENT_ORDER";
        when(orderRepository.existsByOrderNo(orderNo)).thenReturn(false);

        // Act
        boolean result = orderService.existsByOrderNo(orderNo);

        // Assert
        assertThat(result).isFalse();

        verify(orderRepository, times(1)).existsByOrderNo(orderNo);
    }

    // ========== SAVE TESTS ==========

    @Test
    @DisplayName("Should save order items successfully")
    void saveOrderItems_Success() {
        // Arrange
        when(orderItemRepository.saveAll(testOrderItems)).thenReturn(testOrderItems);

        // Act
        List<OrderItem> result = orderService.saveOrderItems(testOrderItems);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(orderItemRepository, times(1)).saveAll(testOrderItems);
    }

    @Test
    @DisplayName("Should save order successfully")
    void saveOrder_Success() {
        // Arrange
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Act
        Order result = orderService.saveOrder(testOrder);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("Should update order successfully")
    void updateOrder_Success() {
        // Arrange
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // Act
        Order result = orderService.updateOrder(testOrder);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(orderRepository, times(1)).save(testOrder);
    }
}
