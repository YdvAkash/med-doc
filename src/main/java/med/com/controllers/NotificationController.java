package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.entity.UserEntity;
import med.com.repository.UserRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows access from HTML page directly
public class NotificationController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String message = request.get("message");

        if (title == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Title and message are required."));
        }

        List<UserEntity> users = userRepository.findByPushTokenIsNotNull();
        List<Map<String, Object>> pushMessages = new ArrayList<>();

        for (UserEntity user : users) {
            if (user.getPushToken() != null && !user.getPushToken().trim().isEmpty()) {
                Map<String, Object> pushMessage = new HashMap<>();
                pushMessage.put("to", user.getPushToken());
                pushMessage.put("title", title);
                pushMessage.put("body", message);
                pushMessage.put("sound", "default");
                pushMessages.add(pushMessage);
            }
        }

        if (pushMessages.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "No users with valid push tokens found."));
        }

        // Send to Expo Push API
        String expoPushUrl = "https://exp.host/--/api/v2/push/send";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(pushMessages, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(expoPushUrl, entity, String.class);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notifications sent successfully.",
                    "expoResponse", response.getBody() != null ? response.getBody() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to send notifications: " + e.getMessage()
            ));
        }
    }
}
