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
    private String title;
    private java.util.List<String> tags;
    private String downloadUrl; // pre-signed URL, populated on GET single
    private java.util.List<MetricDto> metrics;

    @Data
    public static class MetricDto {
        private String name;
        private String value;
        private String unit;
        private String status; // "normal" or "attention"
        private String icon;   // MaterialIcon name
    }
}
