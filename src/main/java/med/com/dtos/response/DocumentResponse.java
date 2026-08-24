package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String originalFilename;
    private Long fileSizeBytes;
    private String fileType;
    private LocalDateTime uploadDate;
    private LocalDate extractedEventDate;
    private String processingStatus;
    private String category;
    private String notes;
    private String downloadUrl; // pre-signed URL, populated on GET single
}
