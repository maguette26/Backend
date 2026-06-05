
package ma.osbt.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String contenu;
    private boolean anonymat;
    private Long consultationId;
}