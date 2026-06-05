package ma.osbt.service.implementation;


import lombok.RequiredArgsConstructor;
import ma.osbt.dto.MessageDTO;
import ma.osbt.dto.SendMessageRequest;
import ma.osbt.entitie.*;
import ma.osbt.repository.ConsultationRepository;
import ma.osbt.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;

    /**
     * Vérifie que l'utilisateur a accès à la consultation ET qu'elle est active.
     */

    public Consultation validateAccess(Long consultationId, Long userId) {
        Consultation consultation = consultationRepository
            .findByIdAndUserId(consultationId, userId)
            .orElseThrow(() -> new AccessDeniedException(
                "Accès refusé à cette consultation"
            ));
        if (consultation.getStatut() != StatutConsultation.CONFIRMEE) {
            throw new IllegalStateException(
                "La consultation n'est pas accessible (statut: "
                + consultation.getStatut() + ")"
            );
        }
        return consultation;
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getHistory(Long consultationId, Long userId) {
        validateAccess(consultationId, userId);
        return messageRepository.findByConsultationId(consultationId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public Message saveMessage(SendMessageRequest request, Personne expediteur) {
        Consultation consultation = validateAccess(request.getConsultationId(), expediteur.getId());

        Message message = new Message();
        message.setContenu(request.getContenu());
        message.setAnonymat(request.isAnonymat());
        message.setDate(new Date());
        message.setHeure(LocalTime.now());
        message.setExpediteur(expediteur);
        message.setConsultation(consultation);

        Long utilisateurId = consultation.getReservation().getUtilisateur().getId();
        if (expediteur.getId().equals(utilisateurId)) {
            message.setDestinataire(consultation.getProfessionnel());
        } else {
            message.setDestinataire(consultation.getReservation().getUtilisateur());
        }
        return messageRepository.save(message);
    }

    public MessageDTO toDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setContenu(m.getContenu());
        dto.setAnonymat(m.isAnonymat());
        dto.setDate(m.getDate());
        dto.setHeure(m.getHeure());
        dto.setConsultationId(m.getConsultation().getIdConsultation());
        dto.setInapproprie(m.isInapproprie());
        dto.setExpediteurId(m.getExpediteur().getId());
        // Respecte l'anonymat
        if (!m.isAnonymat()) {
            dto.setExpediteurNom(m.getExpediteur().getNom() + " " + m.getExpediteur().getPrenom());
        }
        return dto;
    }

    // Exception interne
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String msg) { super(msg); }
    }
}
