package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.AddressRequest;
import com.abel.ecommerce.entity.Address;
import com.abel.ecommerce.exception.AddressNotFoundException;
import com.abel.ecommerce.exception.DefaultAddressException;
import com.abel.ecommerce.filter.RateLimitFilter;
import com.abel.ecommerce.service.AddressService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
 * Controller tests for AddressController
 */
@EnableMethodSecurity
@WebMvcTest(controllers = AddressController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class))
@DisplayName("AddressController Web Layer Tests")
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Address testAddress;
    private AddressRequest testAddressRequest;
    private AddressRequest invalidPhoneAddressRequest;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Create test address
        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUserId(testUserId);
        testAddress.setReceiverName("John Doe");
        testAddress.setReceiverPhone("13812345678");
        testAddress.setProvince("California");
        testAddress.setCity("Los Angeles");
        testAddress.setDistrict("Downtown");
        testAddress.setDetailAddress("123 Main St");
        testAddress.setIsDefault(Address.DEFAULT_ADDRESS);

        // Create test address request with valid phone
        testAddressRequest = new AddressRequest();
        testAddressRequest.setReceiverName("Jane Smith");
        testAddressRequest.setReceiverPhone("13987654321");
        testAddressRequest.setProvince("New York");
        testAddressRequest.setCity("New York");
        testAddressRequest.setDistrict("Manhattan");
        testAddressRequest.setDetailAddress("456 Park Ave");
        testAddressRequest.setIsDefault(Address.NON_DEFAULT_ADDRESS);

        // Create test address request with invalid phone (starts with 0, not 1)
        invalidPhoneAddressRequest = new AddressRequest();
        invalidPhoneAddressRequest.setReceiverName("Invalid Phone User");
        invalidPhoneAddressRequest.setReceiverPhone("0876543210");
        invalidPhoneAddressRequest.setProvince("Shanghai");
        invalidPhoneAddressRequest.setCity("Pudong");
        invalidPhoneAddressRequest.setDistrict("Lujiazui");
        invalidPhoneAddressRequest.setDetailAddress("999 Century Ave");
        invalidPhoneAddressRequest.setIsDefault(Address.NON_DEFAULT_ADDRESS);
    }

    // ========== CREATE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should create address successfully")
    @WithMockUser
    void createAddress_Success() throws Exception {
        // Arrange
        when(addressService.createAddress(eq(testUserId), any(AddressRequest.class))).thenReturn(testAddress);

        // Act & Assert
        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.receiverName").value("John Doe"))
                .andExpect(jsonPath("$.data.province").value("California"));

        verify(addressService, times(1)).createAddress(eq(testUserId), any(AddressRequest.class));
    }

    @Test
    @DisplayName("Should return 400 status when Invalid phone number is provided")
    @WithMockUser
    void createAddress_InvalidPhoneNumber_BadRequest() throws Exception {
        // Arrange
        testAddressRequest.setReceiverPhone("0876543210"); // Invalid phone number

        // Act & Assert
        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.msg").value("Invalid phone number format"));

        verify(addressService, never()).createAddress(eq(testUserId), any(AddressRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when receiver name is blank")
    @WithMockUser
    void createAddress_BlankReceiverName_BadRequest() throws Exception {
        // Arrange
        testAddressRequest.setReceiverName("");

        // Act & Assert
        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(anyLong(), any(AddressRequest.class));
    }

    // ========== UPDATE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should update address successfully")
    @WithMockUser
    void updateAddress_Success() throws Exception {
        // Arrange
        when(addressService.updateAddress(eq(testUserId), eq(1L), any(AddressRequest.class)))
                .thenReturn(testAddress);

        // Act & Assert
        mockMvc.perform(put("/api/addresses/{addressId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(addressService, times(1)).updateAddress(eq(testUserId), eq(1L), any(AddressRequest.class));
    }

    @Test
    @DisplayName("Should return error when updating non-existent address")
    @WithMockUser
    void updateAddress_NotFound() throws Exception {
        // Arrange
        when(addressService.updateAddress(eq(testUserId), eq(999L), any(AddressRequest.class)))
                .thenThrow(new AddressNotFoundException("Address not found"));

        // Act & Assert
        mockMvc.perform(put("/api/addresses/{addressId}", 999L)
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== DELETE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should delete address successfully")
    @WithMockUser
    void deleteAddress_Success() throws Exception {
        // Arrange
        doNothing().when(addressService).deleteAddress(testUserId, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/addresses/{addressId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("Address deleted successfully"));

        verify(addressService, times(1)).deleteAddress(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when deleting last address")
    @WithMockUser
    void deleteAddress_LastAddress_ThrowsException() throws Exception {
        // Arrange
        doThrow(new DefaultAddressException("Cannot delete the last remaining address"))
                .when(addressService).deleteAddress(testUserId, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/addresses/{addressId}", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== GET ADDRESSES TESTS ==========

    @Test
    @DisplayName("Should get all user addresses")
    @WithMockUser
    void getUserAddresses_Success() throws Exception {
        // Arrange
        Address address2 = new Address();
        address2.setId(2L);
        address2.setUserId(testUserId);
        address2.setReceiverName("Bob Smith");
        address2.setProvince("Texas");
        address2.setCity("Houston");

        List<Address> addresses = Arrays.asList(testAddress, address2);
        when(addressService.findAddressesByUserId(testUserId)).thenReturn(addresses);

        // Act & Assert
        mockMvc.perform(get("/api/addresses")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].receiverName").value("John Doe"))
                .andExpect(jsonPath("$.data[1].receiverName").value("Bob Smith"));

        verify(addressService, times(1)).findAddressesByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return empty list when user has no addresses")
    @WithMockUser
    void getUserAddresses_EmptyList() throws Exception {
        // Arrange
        when(addressService.findAddressesByUserId(testUserId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/addresses")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Should get address by ID")
    @WithMockUser
    void getAddressById_Success() throws Exception {
        // Arrange
        when(addressService.findAddressByIdAndUserId(1L, testUserId)).thenReturn(testAddress);

        // Act & Assert
        mockMvc.perform(get("/api/addresses/{addressId}", 1L)
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.receiverName").value("John Doe"));

        verify(addressService, times(1)).findAddressByIdAndUserId(1L, testUserId);
    }

    @Test
    @DisplayName("Should return error when address not found by ID")
    @WithMockUser
    void getAddressById_NotFound() throws Exception {
        // Arrange
        when(addressService.findAddressByIdAndUserId(999L, testUserId))
                .thenThrow(new AddressNotFoundException("Address not found"));

        // Act & Assert
        mockMvc.perform(get("/api/addresses/{addressId}", 999L)
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== GET DEFAULT ADDRESS TESTS ==========

    @Test
    @DisplayName("Should get default address")
    @WithMockUser
    void getDefaultAddress_Success() throws Exception {
        // Arrange
        when(addressService.findDefaultAddress(testUserId)).thenReturn(testAddress);

        // Act & Assert
        mockMvc.perform(get("/api/addresses/default")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.defaultAddress").value(true));

        verify(addressService, times(1)).findDefaultAddress(testUserId);
    }

    @Test
    @DisplayName("Should return error when no default address found")
    @WithMockUser
    void getDefaultAddress_NotFound() throws Exception {
        // Arrange
        when(addressService.findDefaultAddress(testUserId))
                .thenThrow(new AddressNotFoundException("No default address found"));

        // Act & Assert
        mockMvc.perform(get("/api/addresses/default")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== SET DEFAULT ADDRESS TESTS ==========

    @Test
    @DisplayName("Should set default address successfully")
    @WithMockUser
    void setDefaultAddress_Success() throws Exception {
        // Arrange
        doNothing().when(addressService).setDefaultAddress(testUserId, 1L);

        // Act & Assert
        mockMvc.perform(put("/api/addresses/{addressId}/default", 1L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("Default address updated successfully"));

        verify(addressService, times(1)).setDefaultAddress(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when setting non-existent address as default")
    @WithMockUser
    void setDefaultAddress_NotFound() throws Exception {
        // Arrange
        doThrow(new AddressNotFoundException("Address not found"))
                .when(addressService).setDefaultAddress(testUserId, 999L);

        // Act & Assert
        mockMvc.perform(put("/api/addresses/{addressId}/default", 999L)
                        .with(csrf())
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== GET ADDRESS COUNT TESTS ==========

    @Test
    @DisplayName("Should get address count")
    @WithMockUser
    void getAddressCount_Success() throws Exception {
        // Arrange
        when(addressService.getAddressCount(testUserId)).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/addresses/count")
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(3));

        verify(addressService, times(1)).getAddressCount(testUserId);
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate required fields in request")
    @WithMockUser
    void createAddress_InvalidRequest_ValidationError() throws Exception {
        // Arrange
        AddressRequest invalidRequest = new AddressRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(anyLong(), any(AddressRequest.class));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser
    void createAddress_ServiceException_ErrorResponse() throws Exception {
        // Arrange
        when(addressService.createAddress(eq(testUserId), any(AddressRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAddressRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}
