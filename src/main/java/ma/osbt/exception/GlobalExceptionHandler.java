package ma.osbt.exception;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // VALIDATION (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Erreur de validation");

        return ResponseEntity.badRequest().body(Map.of(
                "message", message
        ));
    }

    // 🔥 DUPLICATE EMAIL / DB ERROR (IMPORTANT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDuplicate(DataIntegrityViolationException ex) {

        Throwable root = ex.getRootCause();

        String msg = (root != null) ? root.getMessage() : ex.getMessage();

        System.out.println("🔥 ROOT ERROR: " + msg);

        if (msg != null && msg.contains("unique_email")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Cet email est déjà utilisé."
            ));
        }

        if (msg != null && msg.contains("telephone")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Ce numéro de téléphone est déjà utilisé."
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "message", "Erreur de contrainte en base de données."
        ));
    }
}