package com.abel.ecommerce.service;

import com.stripe.model.Event;

public interface StripeWebhookService {

    /**
     * Process Stripe webhook event
     * @param payload Webhook payload
     * @param signature Stripe signature header
     * @return Event object if processed successfully
     */
    Event processWebhook(String payload, String signature);

    /**
     * Handle checkout session completed event
     * @param event Stripe event
     */
    void handleCheckoutSessionCompleted(Event event);

    /**
     * Handle payment intent payment failed event
     * @param event Stripe event
     */
    void handlePaymentIntentFailed(Event event);

    /**
     * Handle charge refunded event
     * @param event Stripe event
     */
    void handleChargeRefunded(Event event);
}
