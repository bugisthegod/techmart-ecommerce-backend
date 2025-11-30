package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.AddressRequest;
import com.abel.ecommerce.entity.Address;

import java.util.List;
import java.util.Optional;

public interface AddressService {

    /**
     * Create a new address for a user
     * @param userId User ID
     * @param request Address creation request
     * @return Created address
     */
    Address createAddress(Long userId, AddressRequest request);

    /**
     * Update an existing address
     * @param userId User ID
     * @param addressId Address ID
     * @param request Address update request
     * @return Updated address
     */
    Address updateAddress(Long userId, Long addressId, AddressRequest request);

    /**
     * Delete an address
     * @param userId User ID
     * @param addressId Address ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * Set an address as default
     * @param userId User ID
     * @param addressId Address ID
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * Find all addresses for a user
     * @param userId User ID
     * @return List of addresses
     */
    List<Address> findAddressesByUserId(Long userId);

    /**
     * Find the default address for a user
     * @param userId User ID
     * @return Optional containing the default address if it exists
     */
    Address findDefaultAddress(Long userId);

    /**
     * Find an address by ID and user ID
     * @param addressId Address ID
     * @param userId User ID
     * @return Address entity
     */
    Address findAddressByIdAndUserId(Long addressId, Long userId);

    /**
     * Find an address by ID
     * @param addressId Address ID
     * @return Address entity
     */
    Address findAddressById(Long addressId);

    /**
     * Check if user has any addresses
     * @param userId User ID
     * @return true if user has addresses
     */
    boolean hasAddresses(Long userId);

    /**
     * Get count of addresses for a user
     * @param userId User ID
     * @return Number of addresses
     */
    long getAddressCount(Long userId);
}
