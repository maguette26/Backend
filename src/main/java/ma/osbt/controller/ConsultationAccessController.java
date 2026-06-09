package ma.osbt.controller;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.service.ConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint utilisé par la page /access/consultation/:id (scan QR code).
 * Vérifie que l'utilisateur connecté est bien un participant de la consultation.
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationAccessController {

    private final ConsultationService consultationService;

    @GetMapping("/{id}/access")
    public ResponseEntity<?> getConsultationAccess(
            @PathVariable Long id,
            Authentication authentication) {

        // 1. Récupération de la consultation
        Consultation consultation = consultationService.findById(id);
        if (consultation == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Vérification de l'autorisation
        //    Le principal peut être Utilisateur ou ProfessionnelSanteMentale selon votre config Security
        Object principal = authentication.getPrincipal();
        boolean autorise = false;

        if (principal instanceof Utilisateur user) {
            Long userId = user.getId();
            Long reservationUserId = consultation.getReservation() != null
                    && consultation.getReservation().getUtilisateur() != null
                    ? consultation.getReservation().getUtilisateur().getId()
                    : null;
            autorise = userId.equals(reservationUserId);
        } else if (principal instanceof ProfessionnelSanteMentale pro) {
            Long proId = pro.getId();
            Long consultationProId = consultation.getProfessionnel() != null
                    ? consultation.getProfessionnel().getId()
                    : null;
            autorise = proId.equals(consultationProId);
        }

        if (!autorise) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }

        // 3. Construction de la réponse (adapte les getters à vos entités)
        Map<String, Object> response = Map.of(
            "id",                   consultation.getId(),
            "statut",               consultation.getStatut().name(),
            "dateConsultation",     consultation.getDateConsultation().toString(),
            "heure",                consultation.getHeure().toString(),
            "dureeMinutes",         consultation.getDureeMinutes(),
            "prix",                 consultation.getPrix(),
            "professionnelPrenom",  consultation.getProfessionnel().getPrenom(),
            "professionnelNom",     consultation.getProfessionnel().getNom(),
            "utilisateurPrenom",    consultation.getReservation().getUtilisateur().getPrenom(),
            "utilisateurNom",       consultation.getReservation().getUtilisateur().getNom()
        );

        return ResponseEntity.ok(response);
    }
}