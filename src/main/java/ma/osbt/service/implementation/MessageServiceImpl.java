package ma.osbt.service.implementation;

import java.time.Instant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ReservationStatut;
import ma.osbt.repository.ConsultationRepository;
import ma.osbt.repository.MessageRepository;
import ma.osbt.service.MessageService;
import ma.osbt.service.NotificationService;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private NotificationService notificationService;

    private final List<String> motsInterdits = List.of("suicide", "haine", "violence", "insulte");

    private boolean estInapproprie(String contenu) {
        if (contenu == null) return false;
        String lower = contenu.toLowerCase();
        return motsInterdits.stream().anyMatch(lower::contains);
    }

    @Override
    public Message envoyerMessage(Message message) {
        if (message.getContenu() == null) {
            throw new IllegalArgumentException("Le message ne peut pas être vide");
        }

        if (estInapproprie(message.getContenu())) {
            message.setInapproprie(true);
            notificationService.notifierAdmin("Message inapproprié détecté de " + message.getExpediteur().getNom());
        } else {
            message.setInapproprie(false);
        }

        if (message.getDate() == null) message.setDate(new Date());
        if (message.getHeure() == null) message.setHeure(LocalTime.now());

        Message saved = messageRepository.save(message);

        if (!message.isInapproprie() && saved.getDestinataire() != null) {
            notificationService.notifier(
                saved.getDestinataire(),
                "Vous avez reçu un nouveau message de " + saved.getExpediteur().getNom() +
                " :<br><br>" + saved.getContenu()
            );
        }

        return saved;
    }

    @Override
    public List<Message> getMessagesEntre(Personne p1, Personne p2) {
        return messageRepository.findByExpediteurAndDestinataireOrDestinataireAndExpediteurOrderByDateDesc(p1, p2, p1, p2);
    }

    @Override
    public List<Message> getMessagesPourUtilisateur(Personne personne) {
        return messageRepository.findByExpediteurOrDestinataireOrderByDateDesc(personne, personne);
    }

    @Override
    public List<Message> getTousMessages() {
        return messageRepository.findAllByOrderByDateDesc();
    }

    // =================== CHAT PRIVÉ ===================

    public Consultation getConsultationById(Long idConsultation) {
        return consultationRepository.findById(idConsultation)
                .orElseThrow(() -> new RuntimeException("Consultation introuvable"));
    }

    public List<Message> getMessagesParConsultation(Long consultationId, Personne utilisateur) {
        Consultation consultation = getConsultationById(consultationId);

        // Vérifier que l'utilisateur est patient ou médecin
        boolean estParticipant = consultation.getProfessionnel().getId().equals(utilisateur.getId())
                || consultation.getReservation().getUtilisateur().getId().equals(utilisateur.getId());
        if (!estParticipant) {
            throw new RuntimeException("Accès refusé : vous ne participez pas à cette consultation");
        }

        // Vérifier que le chat est ouvert (jour de la consultation)
        LocalDate today = LocalDate.now();
        Instant instant = consultation.getDateConsultation().toInstant();
        LocalDate dateConsult = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        if (today.isBefore(dateConsult)) {
            throw new RuntimeException("Le chat de cette consultation n'est pas encore ouvert");
        }

        return messageRepository.findByConsultationOrderByDateAsc(consultation);
    }

    @Override
    public Message envoyerMessageConsultation(Long consultationId, Personne expediteur, String contenu) {

        if (contenu == null || contenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Message vide");
        }

        Consultation consultation = getConsultationById(consultationId);

        // 🔐 Vérifier participant
        boolean estParticipant =
                consultation.getProfessionnel().getId().equals(expediteur.getId())
                || consultation.getReservation().getUtilisateur().getId().equals(expediteur.getId());

        if (!estParticipant) {
            throw new RuntimeException("Accès refusé");
        }

        // 💳 Vérifier paiement
        if (consultation.getReservation().getStatut() != ReservationStatut.PAYEE) {
            throw new RuntimeException("Consultation non payée");
        }

        // ⏱ Vérifier fenêtre consultation
        LocalDate dateConsult = consultation.getDateConsultation()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDateTime debut = LocalDateTime.of(dateConsult, consultation.getHeure());
        LocalDateTime fin = debut.plusMinutes(consultation.getDureeMinutes());

        if (LocalDateTime.now().isAfter(fin)) {
            throw new RuntimeException("Consultation terminée");
        }

        // 👤 déterminer destinataire
        Personne destinataire =
                expediteur.getId().equals(consultation.getProfessionnel().getId())
                        ? consultation.getReservation().getUtilisateur()
                        : consultation.getProfessionnel();

        // ✉️ créer message
        Message message = new Message();
        message.setConsultation(consultation);
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);
        message.setContenu(contenu);
        message.setDate(new Date());
        message.setHeure(LocalTime.now());

        // ⚠️ filtre amélioré (pas bloquant)
        if (estInapproprie(contenu)) {
            message.setInapproprie(true);
            notificationService.notifierAdmin(
                    "Message sensible détecté (pas bloqué) de " + expediteur.getNom()
            );
        }

        return messageRepository.save(message);
    }
}