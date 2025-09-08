package com.abel.ecommerce.exception;

public class RoleNotFoundException extends BaseException {


    public RoleNotFoundException(String roleCode) {
        super(404, String.format("Role not found with code: %s", roleCode));
    }
}