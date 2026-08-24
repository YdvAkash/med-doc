package med.com.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String messageType;
    private String content;
    private LocalDateTime createdAt;
}
