package ma.osbt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OnlineStatusDTO {
    private Long userId;
    private String username;
    private boolean online;
}