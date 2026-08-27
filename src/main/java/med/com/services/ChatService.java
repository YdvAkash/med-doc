package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.dtos.request.ChatRequest;
import med.com.dtos.request.GeminiChatRequest;
import med.com.dtos.response.ChatMessageResponse;
import med.com.entity.ChatHistoryEntity;
import med.com.entity.DocumentEntity;
import med.com.entity.UserEntity;
import med.com.exceptions.ResourceNotFoundException;
import med.com.repository.ChatHistoryRepository;
import med.com.repository.DocumentRepository;
import med.com.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    private static final int MAX_CONTEXT_CHARS = 15000; // Limit context size to avoid token overflow

    @Transactional
    public ChatMessageResponse askQuestion(String email, ChatRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        // 1. Fetch ALL user's documents that have extracted text
        Page<DocumentEntity> docsPage = documentRepository.findByUserId(
                user.getId(), PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "uploadDate"))
        );

        // 2. Build context from document raw text (direct context stuffing)
        StringBuilder contextBuilder = new StringBuilder();
        int charCount = 0;
        for (DocumentEntity doc : docsPage.getContent()) {
            if (StringUtils.hasText(doc.getRawText())) {
                String docContext = "--- Document: " + doc.getOriginalFilename() + " ---\n" + doc.getRawText() + "\n\n";
                if (charCount + docContext.length() > MAX_CONTEXT_CHARS) {
                    // Truncate remaining text to fit
                    int remaining = MAX_CONTEXT_CHARS - charCount;
                    if (remaining > 100) {
                        contextBuilder.append(docContext, 0, remaining);
                    }
                    break;
                }
                contextBuilder.append(docContext);
                charCount += docContext.length();
            }
        }
        String contextStr = contextBuilder.toString();

        if (!StringUtils.hasText(contextStr)) {
            contextStr = "(No documents have been uploaded or processed yet.)";
        }

        // 3. Fetch recent chat history for conversational memory
        List<ChatHistoryEntity> recentHistory = chatHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        List<ChatHistoryEntity> reverseHistory = new ArrayList<>();
        for (int i = Math.min(recentHistory.size(), 6) - 1; i >= 0; i--) {
            reverseHistory.add(recentHistory.get(i));
        }

        // 4. Build Gemini message list
        List<GeminiChatRequest.Content> contents = new ArrayList<>();

        String systemPrompt = "You are a helpful and professional medical document AI assistant. " +
                "Your job is to answer the user's questions based ONLY on the medical documents provided below. " +
                "If the answer is not found in the documents, clearly say 'This information is not available in your uploaded documents.' " +
                "DO NOT provide medical diagnoses or treatment advice. " +
                "Be concise and accurate.\n\n" +
                "=== USER'S MEDICAL DOCUMENTS ===\n" + contextStr + "\n=== END OF DOCUMENTS ===\n\n";

        // Add conversation history
        for (ChatHistoryEntity hist : reverseHistory) {
            String role = "user_question".equals(hist.getMessageType()) ? "user" : "model";
            contents.add(new GeminiChatRequest.Content(
                    role,
                    List.of(new GeminiChatRequest.Part(hist.getContent()))
            ));
        }

        // Add current question with system context prepended
        String finalMessage = systemPrompt + "User Question: " + request.getMessage();
        contents.add(new GeminiChatRequest.Content("user", List.of(new GeminiChatRequest.Part(finalMessage))));

        GeminiChatRequest geminiRequest = new GeminiChatRequest(contents);

        // 5. Save user's question
        ChatHistoryEntity userMessage = ChatHistoryEntity.builder()
                .user(user)
                .messageType("user_question")
                .content(request.getMessage())
                .relatedDocumentIds("all_docs")
                .build();
        chatHistoryRepository.save(userMessage);

        // 6. Call Gemini
        log.info("Asking Gemini with {} chars of document context for user={}", contextStr.length(), email);
        String aiResponseContent = geminiService.generateChatResponse(geminiRequest);

        // 7. Save AI response
        ChatHistoryEntity aiMessage = ChatHistoryEntity.builder()
                .user(user)
                .messageType("ai_response")
                .content(aiResponseContent)
                .relatedDocumentIds("all_docs")
                .build();
        chatHistoryRepository.save(aiMessage);

        return ChatMessageResponse.builder()
                .id(aiMessage.getId())
                .messageType(aiMessage.getMessageType())
                .content(aiMessage.getContent())
                .createdAt(aiMessage.getCreatedAt())
                .build();
    }

    public Page<ChatMessageResponse> getChatHistory(String email, int page, int size) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(msg -> ChatMessageResponse.builder()
                        .id(msg.getId())
                        .messageType(msg.getMessageType())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build());
    }
}
