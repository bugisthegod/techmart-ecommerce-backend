package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.AddressRequest;
import com.abel.ecommerce.entity.Address;
import com.abel.ecommerce.exception.AddressNotFoundException;
import com.abel.ecommerce.exception.DefaultAddressException;
import com.abel.ecommerce.repository.AddressRepository;
import com.abel.ecommerce.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AddressServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService Unit Tests")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private Address testAddress;
    private AddressRequest testAddressRequest;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Create test address
        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUserId(testUserId);
        testAddress.setReceiverName("John Doe");
        testAddress.setReceiverPhone("1234567890");
        testAddress.setProvince("California");
        testAddress.setCity("Los Angeles");
        testAddress.setDistrict("Downtown");
        testAddress.setDetailAddress("123 Main St");
        testAddress.setIsDefault(Address.DEFAULT_ADDRESS);

        // Create test address request
        testAddressRequest = new AddressRequest();
        testAddressRequest.setReceiverName("Jane Smith");
        testAddressRequest.setReceiverPhone("0987654321");
        testAddressRequest.setProvince("New York");
        testAddressRequest.setCity("New York");
        testAddressRequest.setDistrict("Manhattan");
        testAddressRequest.setDetailAddress("456 Park Ave");
        testAddressRequest.setIsDefault(Address.NON_DEFAULT_ADDRESS);
    }

    // ========== CREATE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should create address successfully for user with no existing addresses")
    void createAddress_FirstAddress_Success() {
        // Arrange
        when(addressRepository.countByUserId(testUserId)).thenReturn(0L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Address result = addressService.createAddress(testUserId, testAddressRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getReceiverName()).isEqualTo("Jane Smith");
        assertThat(result.getIsDefault()).isEqualTo(Address.DEFAULT_ADDRESS); // First address should be default

        verify(addressRepository, times(1)).countByUserId(testUserId);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Should create non-default address when user has existing addresses")
    void createAddress_NonDefaultAddress_Success() {
        // Arrange
        when(addressRepository.countByUserId(testUserId)).thenReturn(1L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.createAddress(testUserId, testAddressRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsDefault()).isEqualTo(Address.NON_DEFAULT_ADDRESS);
        verify(addressRepository, never()).clearDefaultByUserId(anyLong());
    }

    @Test
    @DisplayName("Should create default address and clear existing default")
    void createAddress_NewDefaultAddress_ClearExisting() {
        // Arrange
        testAddressRequest.setIsDefault(Address.DEFAULT_ADDRESS);
        when(addressRepository.countByUserId(testUserId)).thenReturn(1L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(addressRepository).clearDefaultByUserId(testUserId);

        // Act
        Address result = addressService.createAddress(testUserId, testAddressRequest);

        // Assert
        assertThat(result.getIsDefault()).isEqualTo(Address.DEFAULT_ADDRESS);
        verify(addressRepository, times(1)).clearDefaultByUserId(testUserId);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    // ========== UPDATE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should update address successfully")
    void updateAddress_Success() {
        // Arrange
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(2L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Address result = addressService.updateAddress(testUserId, 1L, testAddressRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReceiverName()).isEqualTo("Jane Smith");
        assertThat(result.getProvince()).isEqualTo("New York");
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent address")
    void updateAddress_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(999L, testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress(testUserId, 999L, testAddressRequest))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to unset default on last address")
    void updateAddress_LastAddressToNonDefault_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(1L);
        testAddressRequest.setIsDefault(Address.NON_DEFAULT_ADDRESS);

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress(testUserId, 1L, testAddressRequest))
                .isInstanceOf(DefaultAddressException.class)
                .hasMessageContaining("must have at least one default address");

        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Should update to default address and clear existing defaults")
    void updateAddress_SetAsDefault_Success() {
        // Arrange
        testAddress.setIsDefault(Address.NON_DEFAULT_ADDRESS);
        testAddressRequest.setIsDefault(Address.DEFAULT_ADDRESS);
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(2L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(addressRepository).clearDefaultByUserId(testUserId);

        // Act
        Address result = addressService.updateAddress(testUserId, 1L, testAddressRequest);

        // Assert
        assertThat(result.getIsDefault()).isEqualTo(Address.DEFAULT_ADDRESS);
        verify(addressRepository, times(1)).clearDefaultByUserId(testUserId);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    // ========== DELETE ADDRESS TESTS ==========

    @Test
    @DisplayName("Should delete non-default address successfully")
    void deleteAddress_NonDefaultAddress_Success() {
        // Arrange
        testAddress.setIsDefault(Address.NON_DEFAULT_ADDRESS);
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(2L);
        doNothing().when(addressRepository).deleteById(1L);

        // Act
        addressService.deleteAddress(testUserId, 1L);

        // Assert
        verify(addressRepository, times(1)).deleteById(1L);
        verify(addressRepository, never()).findByUserIdOrderByDefaultAndCreatedAt(anyLong());
    }

    @Test
    @DisplayName("Should delete default address and set another as default")
    void deleteAddress_DefaultAddress_SetNewDefault() {
        // Arrange
        Address otherAddress = new Address();
        otherAddress.setId(2L);
        otherAddress.setUserId(testUserId);
        otherAddress.setIsDefault(Address.NON_DEFAULT_ADDRESS);

        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(2L);
        doNothing().when(addressRepository).deleteById(1L);
        when(addressRepository.findByUserIdOrderByDefaultAndCreatedAt(testUserId))
                .thenReturn(Arrays.asList(otherAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        addressService.deleteAddress(testUserId, 1L);

        // Assert
        verify(addressRepository, times(1)).deleteById(1L);
        verify(addressRepository, times(1)).findByUserIdOrderByDefaultAndCreatedAt(testUserId);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting last address")
    void deleteAddress_LastAddress_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        when(addressRepository.countByUserId(testUserId)).thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() -> addressService.deleteAddress(testUserId, 1L))
                .isInstanceOf(DefaultAddressException.class);

        verify(addressRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent address")
    void deleteAddress_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(999L, testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.deleteAddress(testUserId, 999L))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).deleteById(anyLong());
    }

    // ========== SET DEFAULT ADDRESS TESTS ==========

    @Test
    @DisplayName("Should set default address successfully")
    void setDefaultAddress_Success() {
        // Arrange
        testAddress.setIsDefault(Address.NON_DEFAULT_ADDRESS);
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));
        doNothing().when(addressRepository).clearDefaultByUserId(testUserId);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        addressService.setDefaultAddress(testUserId, 1L);

        // Assert
        verify(addressRepository, times(1)).clearDefaultByUserId(testUserId);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when setting non-existent address as default")
    void setDefaultAddress_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(999L, testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.setDefaultAddress(testUserId, 999L))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).clearDefaultByUserId(anyLong());
        verify(addressRepository, never()).save(any(Address.class));
    }

    // ========== FIND ADDRESS TESTS ==========

    @Test
    @DisplayName("Should find addresses by user ID")
    void findAddressesByUserId_Success() {
        // Arrange
        Address address2 = new Address();
        address2.setId(2L);
        address2.setUserId(testUserId);
        List<Address> addresses = Arrays.asList(testAddress, address2);

        when(addressRepository.findByUserIdOrderByDefaultAndCreatedAt(testUserId)).thenReturn(addresses);

        // Act
        List<Address> result = addressService.findAddressesByUserId(testUserId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
        verify(addressRepository, times(1)).findByUserIdOrderByDefaultAndCreatedAt(testUserId);
    }

    @Test
    @DisplayName("Should find default address")
    void findDefaultAddress_Success() {
        // Arrange
        when(addressRepository.findByUserIdAndIsDefault(testUserId, Address.DEFAULT_ADDRESS))
                .thenReturn(Optional.of(testAddress));

        // Act
        Address result = addressService.findDefaultAddress(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsDefault()).isEqualTo(Address.DEFAULT_ADDRESS);
        verify(addressRepository, times(1)).findByUserIdAndIsDefault(testUserId, Address.DEFAULT_ADDRESS);
    }

    @Test
    @DisplayName("Should throw exception when no default address found")
    void findDefaultAddress_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findByUserIdAndIsDefault(testUserId, Address.DEFAULT_ADDRESS))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.findDefaultAddress(testUserId))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("default address");
    }

    @Test
    @DisplayName("Should find address by ID and user ID")
    void findAddressByIdAndUserId_Success() {
        // Arrange
        when(addressRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(testAddress));

        // Act
        Address result = addressService.findAddressByIdAndUserId(1L, testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(testUserId);
        verify(addressRepository, times(1)).findByIdAndUserId(1L, testUserId);
    }

    @Test
    @DisplayName("Should throw exception when address not found by ID and user ID")
    void findAddressByIdAndUserId_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findByIdAndUserId(999L, testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.findAddressByIdAndUserId(999L, testUserId))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("Should find address by ID")
    void findAddressById_Success() {
        // Arrange
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        // Act
        Address result = addressService.findAddressById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(addressRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when address not found by ID")
    void findAddressById_NotFound_ThrowsException() {
        // Arrange
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.findAddressById(999L))
                .isInstanceOf(AddressNotFoundException.class);
    }

    // ========== HELPER METHOD TESTS ==========

    @Test
    @DisplayName("Should return true when user has addresses")
    void hasAddresses_True() {
        // Arrange
        when(addressRepository.countByUserId(testUserId)).thenReturn(1L);

        // Act
        boolean result = addressService.hasAddresses(testUserId);

        // Assert
        assertThat(result).isTrue();
        verify(addressRepository, times(1)).countByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return false when user has no addresses")
    void hasAddresses_False() {
        // Arrange
        when(addressRepository.countByUserId(testUserId)).thenReturn(0L);

        // Act
        boolean result = addressService.hasAddresses(testUserId);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get address count for user")
    void getAddressCount_Success() {
        // Arrange
        when(addressRepository.countByUserId(testUserId)).thenReturn(3L);

        // Act
        long result = addressService.getAddressCount(testUserId);

        // Assert
        assertThat(result).isEqualTo(3L);
        verify(addressRepository, times(1)).countByUserId(testUserId);
    }
}