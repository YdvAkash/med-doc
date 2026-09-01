package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReferralProgressResponse {
    private String referredUserName;
    private String status;
    private int documentsCompleted;
    private int chatsCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime qualifiedAt;
}
