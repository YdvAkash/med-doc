package med.com.dtos.response;

import lombok.Data;
import java.util.List;

@Data
public class GeminiEmbeddingResponse {
    private Embedding embedding;

    @Data
    public static class Embedding {
        private List<Float> values;
    }
}
