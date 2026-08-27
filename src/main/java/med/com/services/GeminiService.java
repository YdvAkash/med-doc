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

    @lombok.Data
    public static class DocumentAnalysisResult {
        private String title;
        private java.util.List<String> tags;
        private java.util.List<med.com.dtos.response.DocumentResponse.MetricDto> metrics;
    }

    public DocumentAnalysisResult extractDocumentMetadata(String rawText) {
        String prompt = "You are a medical data extraction assistant. Parse the following OCR text from a medical report and extract the document title, relevant tags, and key health metrics (like Blood Sugar, HbA1c, Cholesterol, WBC, etc.).\n" +
                "Return the results STRICTLY as a JSON object matching this format:\n" +
                "{\n" +
                "  \"title\": \"Complete Blood Count\", // A strict 2-3 word name indicating the exact report type (e.g., 'Complete Blood Count', 'X-Ray Chest', 'KFT Report'). Do NOT use long names or raw filenames.\n" +
                "  \"tags\": [\"Dr. Lal PathLabs\", \"High Cholesterol\"], // Exactly 2 specific tags: Tag 1 = Name of the Hospital/Pathology Lab/Doctor. Tag 2 = The major medical finding or focus (e.g., 'High Cholesterol', 'Diabetes', 'Fracture'). Do NOT use generic tags like 'general' or 'medical report'.\n" +
                "  \"metrics\": [\n" +
                "    {\n" +
                "      \"name\": \"Blood Sugar\",\n" +
                "      \"value\": \"108\",\n" +
                "      \"unit\": \"mg/dL\",\n" +
                "      \"status\": \"normal\", // MUST BE exactly \"normal\" or \"attention\" based on standard medical ranges\n" +
                "      \"icon\": \"water-drop\" // Provide a suitable MaterialIcon name (e.g., \"water-drop\", \"science\", \"monitor-heart\", \"favorite\", \"bloodtype\")\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Return ONLY the JSON object without any markdown formatting, backticks, or extra text.\n" +
                "\n" +
                "Raw Text:\n" +
                rawText;

        try {
            GeminiChatRequest.Part part = new GeminiChatRequest.Part();
            part.setText(prompt);
            GeminiChatRequest.Content content = new GeminiChatRequest.Content();
            content.setParts(java.util.Collections.singletonList(part));
            content.setRole("user");
            
            GeminiChatRequest request = new GeminiChatRequest();
            request.setContents(java.util.Collections.singletonList(content));

            String responseText = generateChatResponse(request);
            
            if (responseText.startsWith("Sorry")) {
                log.warn("Gemini API failed (possibly quota exceeded), skipping metadata extraction.");
                return new DocumentAnalysisResult();
            }

            // Clean up possible markdown from LLM
            responseText = responseText.trim();
            if (responseText.startsWith("```json")) {
                responseText = responseText.substring(7);
            }
            if (responseText.startsWith("```")) {
                responseText = responseText.substring(3);
            }
            if (responseText.endsWith("```")) {
                responseText = responseText.substring(0, responseText.length() - 3);
            }
            responseText = responseText.trim();

            return objectMapper.readValue(responseText, DocumentAnalysisResult.class);

        } catch (Exception e) {
            log.error("Error extracting document metadata from text", e);
            return new DocumentAnalysisResult();
        }
    }
}
