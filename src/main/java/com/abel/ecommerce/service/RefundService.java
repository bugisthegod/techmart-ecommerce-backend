package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.response.RefundResponse;

public interface RefundService {

    /**
     * Process refund for a payment
     * @param paymentId Payment ID to refund
     * @param reason Optional refund reason
     * @return Refund response
     */
    RefundResponse processRefund(Long paymentId, String reason);
}
