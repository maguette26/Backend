package ma.osbt.service;

import java.util.List;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;

public interface MessageService {
	public Message envoyerMessage(Message message);
	public List<Message> getMessagesEntre(Personne p1, Personne p2); 
	public List<Message> getMessagesPourUtilisateur(Personne personne);
	public List<Message> getTousMessages();
	public Consultation getConsultationById(Long idConsultation);
	public Message envoyerMessageConsultation(Long consultationId, Personne expediteur, String contenu);
	 

}
