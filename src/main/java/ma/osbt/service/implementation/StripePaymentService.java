package ma.osbt.service.implementation;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import ma.osbt.entitie.Reservation;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.PaymentService;

@Service("stripePaymentService")
public class StripePaymentService implements PaymentService {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Autowired
    private ReservationRepository reservationRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
        System.out.println("✅ Stripe API Key chargée : " + (Stripe.apiKey != null));
    }

    @Override
    public String createPaymentIntent(
            Long amountIgnored,
            String currency,
            String successUrl,
            String cancelUrl,
            Long reservationId
    ) throws StripeException {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        // ==============================
        // 🔥 VALIDATION PRIX
        // ==============================
        if (reservation.getPrix() == null) {
            throw new RuntimeException("Prix de réservation null");
        }

        double prix = reservation.getPrix();

        if (prix <= 0) {
            throw new RuntimeException("Prix invalide pour Stripe : " + prix);
        }

        long amountInCents = Math.round(prix * 100);

        // 🔥 Stripe minimum check (50 centimes EUR)
        if (amountInCents < 50) {
            throw new RuntimeException(
                "Montant trop faible pour Stripe (min 0.50€). Actuel = " + amountInCents + " cents"
            );
        }

        System.out.println("========== STRIPE DEBUG ==========");
        System.out.println("Reservation ID = " + reservationId);
        System.out.println("Prix DB = " + prix);
        System.out.println("Amount (cents) = " + amountInCents);
        System.out.println("Currency = " + currency);
        System.out.println("Stripe API Key loaded = " + (Stripe.apiKey != null));

        // ==============================
        // PAYMENT INTENT
        // ==============================
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency(currency.toLowerCase())
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .putMetadata("reservation_id", reservationId.toString())
                        .setDescription("Réservation #" + reservationId)
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);

        System.out.println("✅ PaymentIntent créé : " + intent.getId());

        return intent.getClientSecret();
    }

    @Override
    public String createPremiumPaymentIntent(
            String planId,
            String currency,
            String successUrl,
            String cancelUrl,
            String userId
    ) {
        throw new UnsupportedOperationException("Premium payment not implemented yet");
    }
}