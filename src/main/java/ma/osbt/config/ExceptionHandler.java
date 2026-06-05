// config/GlobalExceptionHandler.java
package ma.osbt.config;

 
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.*;

import ma.osbt.service.implementation.ChatService;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandler {

    @MessageExceptionHandler(ChatService.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(ChatService.AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @MessageExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }
}