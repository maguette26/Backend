package ma.osbt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveScheduler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 🔥 toutes les 25 secondes
    @Scheduled(fixedRate = 25000)
    public void sendKeepAlive() {
        messagingTemplate.convertAndSend("/topic/ping", "ping");
    }
}