package ma.osbt.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.Date;

@Data
public class MessageDTO {
    private Long id;
    private String contenu;
    private boolean anonymat;
    private Date date;
    private LocalTime heure;
    private Long expediteurId;
    private String expediteurNom;   
    private Long consultationId;
    private boolean inapproprie;
}