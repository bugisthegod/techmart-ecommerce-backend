package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.AddressRequest;
import com.abel.ecommerce.entity.Address;
import com.abel.ecommerce.exception.AddressNotFoundException;
import com.abel.ecommerce.exception.DefaultAddressException;
import com.abel.ecommerce.repository.AddressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional
    public Address createAddress(Long userId, AddressRequest request) {
        Address address = new Address();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);

        // Check if user has at least one address
        if (!hasAddresses(userId)) {
            address.setIsDefault(Address.DEFAULT_ADDRESS);
        }

        // Check if new address is default
        if (Address.DEFAULT_ADDRESS.equals(request.getIsDefault())) {
            // Clear default address
            addressRepository.clearDefaultByUserId(userId);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address existingAddress = findAddressByIdAndUserId(addressId, userId);

        // Copy basic properties
        BeanUtils.copyProperties(request, existingAddress, "id", "userId", "createdAt");

        // if user want to change the last address as non default
        if (getAddressCount(userId) == 1 && Address.NON_DEFAULT_ADDRESS.equals(request.getIsDefault()))
            throw new DefaultAddressException("You must " +
                    "have at least one default address");

        // Change other addresses as non default
        if (Address.DEFAULT_ADDRESS.equals(request.getIsDefault())) addressRepository.clearDefaultByUserId(userId);


        return addressRepository.save(existingAddress);
    }


    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUserId(addressId, userId);
        boolean wasDefault = address.isDefaultAddress();

        // If this address is the last address
        if (getAddressCount(userId) <= 1) throw DefaultAddressException.cannotDeleteLastAddress();

        // Delete this address at first, because remainingAddresses will contain the delete one
        addressRepository.deleteById(addressId);

        // If this address is default address, set other address as default
        if (wasDefault) {
            List<Address> remainingAddresses  = findAddressesByUserId(userId);
            if (!remainingAddresses .isEmpty()){
                Address newDefault = remainingAddresses .get(0);
                newDefault.setIsDefault(Address.DEFAULT_ADDRESS);
                addressRepository.save(newDefault);
            }
        }

    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUserId(addressId, userId);

        // Clear default address
        addressRepository.clearDefaultByUserId(userId);

        //Set address as default
        address.setIsDefault(Address.DEFAULT_ADDRESS);

        addressRepository.save(address);
    }

    public List<Address> findAddressesByUserId(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAndCreatedAt(userId);
    }

    public Optional<Address> findDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefault(userId, Address.DEFAULT_ADDRESS);
    }

    public Address findAddressByIdAndUserId(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found or doesn't belong to user"));
    }

    public Address findAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    public boolean hasAddresses(Long userId) {
        return addressRepository.countByUserId(userId) > 0;
    }

    public long getAddressCount(Long userId) {
        return addressRepository.countByUserId(userId);
    }
}
