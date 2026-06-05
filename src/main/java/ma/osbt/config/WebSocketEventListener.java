// config/WebSocketEventListener.java
package ma.osbt.config;

import lombok.RequiredArgsConstructor;
import ma.osbt.dto.ChatNotification;
import ma.osbt.dto.OnlineStatusDTO;
import ma.osbt.entitie.Personne;
import ma.osbt.service.implementation.OnlineStatusService;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Personne user = (Personne) accessor.getUser();
        if (user == null) return;

        onlineStatusService.userConnected(user.getId(), accessor.getSessionId());

        messagingTemplate.convertAndSend(
            "/topic/online-status",
            new ChatNotification(
                ChatNotification.Type.USER_ONLINE,
                new OnlineStatusDTO(user.getId(), user.getUsername(), true)
            )
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = onlineStatusService.userDisconnected(accessor.getSessionId());
        if (userId == null) return;

        Personne user = (Personne) accessor.getUser();
        String username = user != null ? user.getUsername() : "unknown";

        messagingTemplate.convertAndSend(
            "/topic/online-status",
            new ChatNotification(
                ChatNotification.Type.USER_OFFLINE,
                new OnlineStatusDTO(userId, username, false)
            )
        );
    }
}