package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.request.ChatRequest;
import med.com.dtos.response.ApiResponse;
import med.com.dtos.response.ChatMessageResponse;
import med.com.services.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final med.com.services.SubscriptionService subscriptionService;
    private final med.com.repository.UserRepository userRepository;

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(
            @RequestBody ChatRequest request,
            Principal principal
    ) {
        med.com.entity.UserEntity user = userRepository.findByEmail(principal.getName()).orElseThrow();
        if (!subscriptionService.canChat(user)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("success", false, "message", "Monthly quota is completed, you need to upgrade your account."));
        }

        ChatMessageResponse response = chatService.askQuestion(principal.getName(), request);
        subscriptionService.incrementChatCount(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        Page<ChatMessageResponse> history = chatService.getChatHistory(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
