// controller/ChatRestController.java
package ma.osbt.controller;

import lombok.RequiredArgsConstructor;
import ma.osbt.dto.MessageDTO;
import ma.osbt.entitie.Personne;
import ma.osbt.service.implementation.ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    /**
     * GET /api/chat/{consultationId}/history
     * Charge l'historique des messages d'une consultation
     */
    @GetMapping("/{consultationId}/history")
    public ResponseEntity<List<MessageDTO>> getHistory(
            @PathVariable Long consultationId,
            @AuthenticationPrincipal Personne currentUser) {

        List<MessageDTO> history = chatService.getHistory(consultationId, currentUser.getId());
        return ResponseEntity.ok(history);
    }
}