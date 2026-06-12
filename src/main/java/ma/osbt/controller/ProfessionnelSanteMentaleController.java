package ma.osbt.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.StatutValidation;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.ProfessionnelSanteMentaleRepository;
import ma.osbt.service.ConsultationService;
import ma.osbt.service.ProfessionnelSanteService;

@RestController
@RequestMapping("/api/professionnels")
public class ProfessionnelSanteMentaleController {

    @Autowired
    private ProfessionnelSanteService service;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private ProfessionnelSanteMentaleRepository professionnelRepository;

    @Autowired
    private Cloudinary cloudinary;

    @PostMapping("/inscription")
    public ResponseEntity<?> inscrireProfessionnel(
        @RequestParam("specialite") String specialite,
        @RequestParam("document") MultipartFile documentFile,
        @RequestParam("nom") String nom,
        @RequestParam("prenom") String prenom,
        @RequestParam("email") String email,
        @RequestParam("motDePasse") String motDePasse,
        @RequestParam("telephone") String telephone
    ) {
        if (!specialite.equalsIgnoreCase("psychiatrie") && !specialite.equalsIgnoreCase("psychologie")) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Spécialité invalide. Seules 'psychiatrie' ou 'psychologie' sont acceptées."
            ));
        }
        if (professionnelRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet email est déjà utilisé !"));
        }
        if (professionnelRepository.existsByTelephone(telephone)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ce numéro de téléphone est déjà utilisé !"));
        }

        try {
            String cloudinaryUrl = saveFile(documentFile);

            ProfessionnelSanteMentale professionnel = new ProfessionnelSanteMentale();
            professionnel.setSpecialite(specialite);
            professionnel.setDocumentJustificatif(cloudinaryUrl);
            professionnel.setStatutValidation(StatutValidation.EN_ATTENTE);
            professionnel.setNom(nom);
            professionnel.setPrenom(prenom);
            professionnel.setEmail(email);
            professionnel.setMotDePasse(new BCryptPasswordEncoder().encode(motDePasse));
            professionnel.setTelephone(telephone);
            professionnel.setRole(
                specialite.equalsIgnoreCase("psychiatrie") ? Role.PSYCHIATRE : Role.PSYCHOLOGUE
            );

            service.saveProfessionnel(professionnel);
            return ResponseEntity.ok(Map.of("message", "Inscription réussie, en attente de validation."));

        } catch (Exception e) {
            e.printStackTrace();
            String causeMessage = "Erreur serveur. Veuillez réessayer.";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("unique_email") || e.getMessage().contains("email")) {
                    causeMessage = "Cet email est déjà utilisé !";
                } else if (e.getMessage().contains("telephone")) {
                    causeMessage = "Ce numéro de téléphone est déjà utilisé !";
                } else {
                    causeMessage = "Erreur : " + e.getMessage();
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", causeMessage));
        }
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<ProfessionnelSanteMentale>> getProfessionnelsEnAttente() {
        return ResponseEntity.ok(service.getProfessionnelsEnAttente());
    }

    @GetMapping("/tous")
    public ResponseEntity<List<ProfessionnelSanteMentale>> getAllProfessionnels() {
        return ResponseEntity.ok(service.getAllProfessionnels());
    }

    @PatchMapping("/validation/{id}")
    public ResponseEntity<?> updateValidation(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean valide = body.get("valide");
            if (valide == null) {
                return ResponseEntity.badRequest().body("Le champ 'valide' est requis.");
            }
            StatutValidation statut = valide ? StatutValidation.VALIDE : StatutValidation.REFUSE;
            ProfessionnelSanteMentale updated = service.updateStatutValidation(id, statut);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/prix-consultation")
    public ResponseEntity<?> definirPrixConsultation(@AuthenticationPrincipal ProfessionnelSanteMentale pro,
                                                     @RequestParam Double nouveauPrix) {
        if (nouveauPrix == null || nouveauPrix <= 0) {
            return ResponseEntity.badRequest().body("Le prix doit être supérieur à 0.");
        }
        pro.setPrixConsultation(nouveauPrix);
        professionnelRepository.save(pro);
        return ResponseEntity.ok("Prix de consultation mis à jour avec succès.");
    }

    @GetMapping("/prix-consultation")
    public ResponseEntity<Double> getPrixConsultation(@AuthenticationPrincipal ProfessionnelSanteMentale pro) {
        return ResponseEntity.ok(pro.getPrixConsultation());
    }

    @GetMapping("/mes-reservations")
    public List<Map<String, Object>> getReservationsPourProfessionnel(
            @AuthenticationPrincipal Personne personneConnectee) {

        if (!(personneConnectee instanceof ProfessionnelSanteMentale professionnel)) {
            throw new RuntimeException("Seuls les professionnels peuvent accéder à leurs réservations");
        }

        List<Consultation> consultations =
                consultationService.getConsultationsParProfessionnelId(professionnel.getId());

        return consultations.stream().map(consultation -> {
            Map<String, Object> map = new HashMap<>();

            // ── Identifiant ────────────────────────────────────────────
            map.put("id", consultation.getIdConsultation());

            // ── Statut global ──────────────────────────────────────────
            map.put("statut", consultation.getStatut() != null
                    ? consultation.getStatut().name() : null);

            // ── Notes ──────────────────────────────────────────────────
            map.put("notesProfessionnel", consultation.getNotesProfessionnel());
            map.put("notesUtilisateur",   consultation.getNotesUtilisateur());

            // ── Utilisateur ────────────────────────────────────────────
            if (consultation.getReservation() != null
                    && consultation.getReservation().getUtilisateur() != null) {

                Utilisateur utilisateur = consultation.getReservation().getUtilisateur();
                Map<String, Object> utilisateurMap = new HashMap<>();
                utilisateurMap.put("nom",    utilisateur.getNom());
                utilisateurMap.put("prenom", utilisateur.getPrenom());
                utilisateurMap.put("email",  utilisateur.getEmail());
                map.put("utilisateur", utilisateurMap);

                // ── Date / heure de réservation ────────────────────────
                map.put("dateReservation",  consultation.getReservation().getDateReservation());
                map.put("heureReservation", consultation.getReservation().getHeureReservation());
            }

            // ── Sous-objet consultation (date + heure planifiées) ──────
            // LocalTime est sérialisé nativement par Jackson :
            // { "hour": 10, "minute": 30, "second": 0, "nano": 0 }
            // LocalDate / Date est sérialisé en timestamp ms (Long) ou "YYYY-MM-DD"
            Map<String, Object> consultationMap = new HashMap<>();
            consultationMap.put("date",   consultation.getDateConsultation()); // LocalDate ou java.util.Date
            consultationMap.put("heure",  consultation.getHeure());            // LocalTime — pas formaté
            consultationMap.put("prix",   consultation.getPrix());
            consultationMap.put("statut", consultation.getStatut() != null
                    ? consultation.getStatut().name() : null);
            map.put("consultation", consultationMap);

            return map;
        }).toList();
    }

    // ── Méthode privée ──────────────────────────────────────────────────────────
    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fichier vide");
        }

        Map uploadResult = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap("folder", "professionnels", "resource_type", "auto")
        );

        String url = (String) uploadResult.get("secure_url");

        if (url == null || url.isEmpty()) {
            throw new RuntimeException("Cloudinary n'a pas retourné d'URL");
        }

        return url;
    }
}