package com.abel.ecommerce.exception;

public class AddressNotFoundException extends BaseException {
    
    public AddressNotFoundException(String message) {
        super(404, message);
    }
    
    public AddressNotFoundException(Long addressId) {
        super(404, "Address not found with ID: " + addressId);
    }
    
    public AddressNotFoundException(Long addressId, String field) {
        super(404, String.format("Address not found with %s: %s", field, addressId));
    }
}
