package ma.osbt.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.StatutConsultation;
@Repository

public interface ConsultationRepository  extends JpaRepository<Consultation, Long>{

	List<Consultation> findByProfessionnelAndDateConsultation(ProfessionnelSanteMentale pro, Date valueOf);
	List<Consultation> findByReservationUtilisateurId(Long utilisateurId);

	Optional<Consultation> findByReservation(Reservation reservation);
    List<Consultation> findByReservation_Utilisateur_Id(Long utilisateurId);

    List<Consultation> findByReservation_Utilisateur_IdAndStatut(Long utilisateurId, StatutConsultation statut);
    List<Consultation> findByStatut(String statut);
	List<Consultation> findByStatut(StatutConsultation confirmee);
	Optional<Consultation> findByReservation_Id(Long reservationId);
	List<Consultation> findByProfessionnelId(Long professionnelId);
	

	// Pour ChatService.validateAccess() — cherche la consultation par id ET vérifie que l'user en est membre
	@Query("""
	    SELECT c FROM Consultation c
	    WHERE c.idConsultation = :id
	    AND (
	        c.reservation.utilisateur.id = :userId
	        OR c.professionnel.id = :userId
	    )
	""")
	Optional<Consultation> findByIdAndUserId(
	    @Param("id") Long id,
	    @Param("userId") Long userId
	);

	 

	// Pour l'historique par consultation
	@Query("SELECT m FROM Message m WHERE m.consultation.idConsultation = :id ORDER BY m.date ASC, m.heure ASC")
	List<Message> findByConsultationId(@Param("id") Long consultationId);
	// ☝️ Ceci va dans MessageRepository


}
