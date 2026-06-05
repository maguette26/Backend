package ma.osbt.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
@Repository

public interface MessageRepository extends JpaRepository<Message,Long>{
	List<Message> findByExpediteurId(Long id);
    List<Message> findByDestinataireId(Long id);
    List<Message> findByExpediteurOrDestinataireOrderByDateDesc(Personne expediteur, Personne destinataire);
    List<Message> findByExpediteurAndDestinataireOrDestinataireAndExpediteurOrderByDateDesc(
    	    Personne exp1, Personne dest1, Personne dest2, Personne exp2
    	);
	List<Message> findAllByOrderByDateDesc();
	 

    @Query("SELECT m FROM Message m WHERE m.consultation.idConsultation = :consultationId ORDER BY m.date ASC, m.heure ASC")
    List<Message> findByConsultationId(@Param("consultationId") Long consultationId);
	List<Message> findByConsultationOrderByDateAsc(Consultation consultation);


	
}
