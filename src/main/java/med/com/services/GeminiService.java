package med.com.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.dtos.request.GeminiChatRequest;
import med.com.dtos.response.GeminiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_CHAT_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

    public String generateChatResponse(GeminiChatRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Calling Gemini Chat API...");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_CHAT_URL + "?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                GeminiChatResponse chatResponse = objectMapper.readValue(response.body(), GeminiChatResponse.class);
                if (chatResponse.getCandidates() != null && !chatResponse.getCandidates().isEmpty()) {
                    String text = chatResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
                    log.info("Gemini Chat API returned successfully.");
                    return text;
                }
            } else {
                log.error("Gemini Chat API error: {} - {}", response.statusCode(), response.body());
                return "Sorry, I encountered an error while processing your request. Please try again.";
            }
        } catch (Exception e) {
            log.error("Exception calling Gemini Chat", e);
            return "Sorry, an unexpected error occurred. Please try again later.";
        }
        return "Sorry, I couldn't process your request.";
    }
}
