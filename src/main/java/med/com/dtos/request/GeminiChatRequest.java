package med.com.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiChatRequest {
    private List<Content> contents;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private String role; // "user" or "model"
        private List<Part> parts;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text;
    }
}
