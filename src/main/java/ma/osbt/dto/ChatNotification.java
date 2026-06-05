package ma.osbt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatNotification {
    public enum Type { NEW_MESSAGE, USER_ONLINE, USER_OFFLINE, ERROR }

    private Type type;
    private Object payload;
}