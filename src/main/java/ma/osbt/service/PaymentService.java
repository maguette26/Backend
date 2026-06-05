package ma.osbt.service;

public interface PaymentService {

    // Paiement d'une réservation
    String createPaymentIntent(Long amountInCents, String currency,
                               String successUrl, String cancelUrl,
                               Long reservationId) throws Exception;

    // Paiement Premium
    String createPremiumPaymentIntent(String planId, String currency,
                                     String successUrl, String cancelUrl,
                                     String userId) throws Exception;
}