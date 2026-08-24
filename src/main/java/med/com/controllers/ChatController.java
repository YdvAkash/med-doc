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

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> askQuestion(
            @RequestBody ChatRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatService.askQuestion(principal.getName(), request);
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
