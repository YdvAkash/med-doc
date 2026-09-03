package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FolderResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private int documentCount;
}
