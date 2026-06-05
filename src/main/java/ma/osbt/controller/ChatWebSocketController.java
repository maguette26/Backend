// controller/ChatWebSocketController.java
package ma.osbt.controller;

import lombok.RequiredArgsConstructor;
import ma.osbt.dto.*;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
import ma.osbt.service.implementation.ChatService;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * STOMP: /app/chat.send
     * Reçoit un message, le persiste, le broadcast aux 2 participants
     */
    @MessageMapping("/chat.send")
    public void sendMessage(
            SendMessageRequest request,
            @AuthenticationPrincipal Personne currentUser) {

        try {
            Message saved = chatService.saveMessage(request, currentUser);
            MessageDTO dto = chatService.toDTO(saved);
            ChatNotification notification = new ChatNotification(ChatNotification.Type.NEW_MESSAGE, dto);

            String topic = "/topic/consultation." + request.getConsultationId();
            messagingTemplate.convertAndSend(topic, notification);

        } catch (ChatService.AccessDeniedException e) {
            // Renvoie l'erreur uniquement à l'émetteur
            messagingTemplate.convertAndSendToUser(
                currentUser.getUsername(),
                "/queue/errors",
                new ChatNotification(ChatNotification.Type.ERROR, e.getMessage())
            );
        }
    }
}