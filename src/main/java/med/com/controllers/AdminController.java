package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.entity.UserEntity;
import med.com.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        
        List<Map<String, Object>> userDTOs = users.stream().map(user -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            map.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            map.put("subscriptionTier", user.getSubscriptionTier() != null ? user.getSubscriptionTier() : "FREE");
            map.put("isActive", user.getIsActive() != null ? user.getIsActive() : false);
            map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<UserEntity> users = userRepository.findAll();
        
        long totalUsers = users.size();
        long activeSubscriptions = users.stream()
                .filter(u -> "PRO".equalsIgnoreCase(u.getSubscriptionTier()) || "BASIC".equalsIgnoreCase(u.getSubscriptionTier()))
                .count();
                
        // Safe unboxing of Boolean
        long dailyReach = users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("dailyReach", dailyReach);
        stats.put("activeSubscriptions", activeSubscriptions);

        return ResponseEntity.ok(stats);
    }
}
