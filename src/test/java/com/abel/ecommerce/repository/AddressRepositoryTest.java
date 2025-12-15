package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Address;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AddressRepository
 *
 * Tests address management including default address handling,
 * user ownership validation, and custom query methods
 */
@DataJpaTest
@DisplayName("AddressRepository Integration Tests")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private EntityManager entityManager;

    private Address address1;
    private Address address2;
    private Address address3;
    private Long testUserId1 = 1L;
    private Long testUserId2 = 2L;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();

        // Create addresses for user 1
        address1 = new Address();
        address1.setUserId(testUserId1);
        address1.setReceiverName("John Doe");
        address1.setReceiverPhone("13812345678");
        address1.setProvince("California");
        address1.setCity("Los Angeles");
        address1.setDistrict("Downtown");
        address1.setDetailAddress("123 Main St");
        address1.setIsDefault(Address.DEFAULT_ADDRESS);

        address2 = new Address();
        address2.setUserId(testUserId1);
        address2.setReceiverName("Jane Doe");
        address2.setReceiverPhone("13987654321");
        address2.setProvince("New York");
        address2.setCity("New York");
        address2.setDistrict("Manhattan");
        address2.setDetailAddress("456 Park Ave");
        address2.setIsDefault(Address.NON_DEFAULT_ADDRESS);

        // Create address for user 2
        address3 = new Address();
        address3.setUserId(testUserId2);
        address3.setReceiverName("Bob Smith");
        address3.setReceiverPhone("13666666666");
        address3.setProvince("Texas");
        address3.setCity("Houston");
        address3.setDistrict("Midtown");
        address3.setDetailAddress("789 Oak Rd");
        address3.setIsDefault(Address.DEFAULT_ADDRESS);

        addressRepository.save(address1);
        addressRepository.save(address2);
        addressRepository.save(address3);
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find addresses by user ID ordered by default and created time")
    void findByUserIdOrderByDefaultAndCreatedAt() {
        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAndCreatedAt(testUserId1);

        assertThat(addresses).hasSize(2);
        // Default address should come first
        assertThat(addresses.get(0).isDefaultAddress()).isTrue();
        assertThat(addresses.get(0).getReceiverName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should find user's default address")
    void findByUserIdAndIsDefault() {
        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefault(
                testUserId1,
                Address.DEFAULT_ADDRESS
        );

        assertThat(defaultAddress).isPresent();
        assertThat(defaultAddress.get().getReceiverName()).isEqualTo("John Doe");
        assertThat(defaultAddress.get().isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when user has no default address")
    void findByUserIdAndIsDefault_NoDefault() {
        // Clear default
        addressRepository.clearDefaultByUserId(testUserId1);

        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefault(
                testUserId1,
                Address.DEFAULT_ADDRESS
        );

        assertThat(defaultAddress).isEmpty();
    }

    @Test
    @DisplayName("Should check if user has default address")
    void existsByUserIdAndIsDefault() {
        boolean user1HasDefault = addressRepository.existsByUserIdAndIsDefault(
                testUserId1,
                Address.DEFAULT_ADDRESS
        );
        boolean user2HasDefault = addressRepository.existsByUserIdAndIsDefault(
                testUserId2,
                Address.DEFAULT_ADDRESS
        );

        assertThat(user1HasDefault).isTrue();
        assertThat(user2HasDefault).isTrue();
    }

    @Test
    @DisplayName("Should count addresses by user ID")
    void countByUserId() {
        long user1Count = addressRepository.countByUserId(testUserId1);
        long user2Count = addressRepository.countByUserId(testUserId2);
        long user3Count = addressRepository.countByUserId(999L);

        assertThat(user1Count).isEqualTo(2);
        assertThat(user2Count).isEqualTo(1);
        assertThat(user3Count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should clear all default addresses for a user")
    void clearDefaultByUserId() {
        addressRepository.clearDefaultByUserId(testUserId1);
        addressRepository.flush(); // Force write to database, do not clear cache
        entityManager.clear(); // Force clear cache (persistence context)

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAndCreatedAt(testUserId1);

        assertThat(addresses).hasSize(2);
        assertThat(addresses).allMatch(a -> !a.isDefaultAddress());
    }

    @Test
    @DisplayName("Should update default status for specific address")
    void updateDefaultStatus() {
        // Set address2 as default
        addressRepository.updateDefaultStatus(
                address2.getId(),
                testUserId1,
                Address.DEFAULT_ADDRESS
        );
        addressRepository.flush();
        entityManager.clear();

        Address updated = addressRepository.findById(address2.getId()).orElseThrow();
        assertThat(updated.isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("Should check if address belongs to specific user")
    void existsByIdAndUserId() {
        boolean belongsToUser1 = addressRepository.existsByIdAndUserId(
                address1.getId(),
                testUserId1
        );
        boolean belongsToUser2 = addressRepository.existsByIdAndUserId(
                address1.getId(),
                testUserId2
        );

        assertThat(belongsToUser1).isTrue();
        assertThat(belongsToUser2).isFalse();
    }

    @Test
    @DisplayName("Should find address by ID and user ID")
    void findByIdAndUserId() {
        Optional<Address> found = addressRepository.findByIdAndUserId(
                address1.getId(),
                testUserId1
        );
        Optional<Address> notFound = addressRepository.findByIdAndUserId(
                address1.getId(),
                testUserId2
        );

        assertThat(found).isPresent();
        assertThat(found.get().getReceiverName()).isEqualTo("John Doe");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should delete all addresses for a user")
    void deleteByUserId() {
        addressRepository.deleteByUserId(testUserId1);
        addressRepository.flush();

        long count = addressRepository.countByUserId(testUserId1);
        assertThat(count).isEqualTo(0);

        // User 2's addresses should remain
        long user2Count = addressRepository.countByUserId(testUserId2);
        assertThat(user2Count).isEqualTo(1);
    }

    // ========== DEFAULT ADDRESS MANAGEMENT TESTS ==========

    @Test
    @DisplayName("Should handle default address switching")
    void switchDefaultAddress() {
        // Clear all defaults first
        addressRepository.clearDefaultByUserId(testUserId1);
        addressRepository.flush();

        // Set address2 as default
        addressRepository.updateDefaultStatus(
                address2.getId(),
                testUserId1,
                Address.DEFAULT_ADDRESS
        );
        addressRepository.flush();

        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefault(
                testUserId1,
                Address.DEFAULT_ADDRESS
        );

        assertThat(defaultAddress).isPresent();
        assertThat(defaultAddress.get().getId()).isEqualTo(address2.getId());
        assertThat(defaultAddress.get().getReceiverName()).isEqualTo("Jane Doe");
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle user with no addresses")
    void findByUserId_EmptyResult() {
        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAndCreatedAt(999L);
        assertThat(addresses).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple non-default addresses")
    void multipleNonDefaultAddresses() {
        addressRepository.clearDefaultByUserId(testUserId1);
        addressRepository.flush();
        entityManager.clear();


        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAndCreatedAt(testUserId1);

        assertThat(addresses).hasSize(2);
        assertThat(addresses).noneMatch(Address::isDefaultAddress);
    }
}