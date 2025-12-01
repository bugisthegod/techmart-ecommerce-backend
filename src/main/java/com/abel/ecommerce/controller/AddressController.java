package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.AddressRequest;
import com.abel.ecommerce.dto.response.AddressResponse;
import com.abel.ecommerce.entity.Address;
import com.abel.ecommerce.exception.AddressNotFoundException;
import com.abel.ecommerce.exception.DefaultAddressException;
import com.abel.ecommerce.service.AddressService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Address Management", description = "User shipping address management")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "Create new address", description = "Add a new shipping address for user")
    @PostMapping
    public ResponseResult<AddressResponse> createAddress(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Address data") @Valid @RequestBody AddressRequest request) {
        try {
            Address address = addressService.createAddress(userId, request);
            AddressResponse response = convertToResponse(address);
            return ResponseResult.ok(response);
        }
        catch (Exception e) {
            log.error("Unexpected error creating address - userId: {}", userId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Update address", description = "Update an existing address")
    @PutMapping("/{addressId}")
    public ResponseResult<AddressResponse> updateAddress(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Address ID") @PathVariable Long addressId,
            @Parameter(description = "Updated address data") @Valid @RequestBody AddressRequest request) {
        try {
            Address address = addressService.updateAddress(userId, addressId, request);
            AddressResponse response = convertToResponse(address);
            return ResponseResult.ok(response);
        }
        catch (AddressNotFoundException e) {
            log.error("Address not found for update - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (DefaultAddressException e) {
            log.error("Default address exception during update - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error updating address - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Delete address", description = "Delete a shipping address")
    @DeleteMapping("/{addressId}")
    public ResponseResult<String> deleteAddress(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        try {
            addressService.deleteAddress(userId, addressId);
            return ResponseResult.ok("Address deleted successfully");
        }
        catch (AddressNotFoundException e) {
            log.error("Address not found for deletion - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (DefaultAddressException e) {
            log.error("Cannot delete default address - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error deleting address - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get user addresses", description = "Get all addresses for a user")
    @GetMapping
    public ResponseResult<List<AddressResponse>> getUserAddresses(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            List<Address> addresses = addressService.findAddressesByUserId(userId);
            List<AddressResponse> responses = addresses.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseResult.ok(responses);
        }
        catch (Exception e) {
            log.error("Unexpected error getting user addresses - userId: {}", userId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get address by ID", description = "Get specific address details")
    @GetMapping("/{addressId}")
    public ResponseResult<AddressResponse> getAddressById(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        try {
            Address address = addressService.findAddressByIdAndUserId(addressId, userId);
            AddressResponse response = convertToResponse(address);
            return ResponseResult.ok(response);
        }
        catch (AddressNotFoundException e) {
            log.error("Address not found - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error getting address by ID - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get default address", description = "Get user's default shipping address")
    @GetMapping("/default")
    public ResponseResult<AddressResponse> getDefaultAddress(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            Address defaultAddress = addressService.findDefaultAddress(userId);

            AddressResponse response = convertToResponse(defaultAddress);
            return ResponseResult.ok(response);
        }
        catch (AddressNotFoundException e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), "No default address found");
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Set default address", description = "Set an address as default")
    @PutMapping("/{addressId}/default")
    public ResponseResult<String> setDefaultAddress(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        try {
            addressService.setDefaultAddress(userId, addressId);
            return ResponseResult.ok("Default address updated successfully");
        }
        catch (AddressNotFoundException e) {
            log.error("Address not found when setting default - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error setting default address - userId: {}, addressId: {}", userId, addressId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get address count", description = "Get total number of addresses for user")
    @GetMapping("/count")
    public ResponseResult<Long> getAddressCount(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            long count = addressService.getAddressCount(userId);
            return ResponseResult.ok(count);
        }
        catch (Exception e) {
            log.error("Unexpected error getting address count - userId: {}", userId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    private AddressResponse convertToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        BeanUtils.copyProperties(address, response);

        // Set computed fields
        response.setFullAddress(address.getFullAddress());
        response.setDefaultAddress(address.isDefaultAddress());

        return response;
    }
}
