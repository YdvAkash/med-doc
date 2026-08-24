package med.com.dtos.response;

import lombok.Data;
import java.util.List;

@Data
public class GeminiChatResponse {
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Content content;
    }

    @Data
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    public static class Part {
        private String text;
    }
}
