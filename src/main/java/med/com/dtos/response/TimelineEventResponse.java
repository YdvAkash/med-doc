package med.com.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import med.com.entity.TimelineEventEntity;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventResponse {
    private Long id;
    private LocalDate eventDate;
    private String eventType;
    private String title;
    private String description;
    private String severity;
    private Long relatedDocumentId;
    private DocumentResponse document;

    public static TimelineEventResponse fromEntity(TimelineEventEntity entity) {
        return TimelineEventResponse.builder()
                .id(entity.getId())
                .eventDate(entity.getEventDate())
                .eventType(entity.getEventType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .severity(entity.getSeverity())
                .relatedDocumentId(entity.getRelatedDocument() != null ? entity.getRelatedDocument().getId() : null)
                .document(entity.getRelatedDocument() != null ? mapToDocumentResponse(entity.getRelatedDocument()) : null)
                .build();
    }
    
    private static DocumentResponse mapToDocumentResponse(med.com.entity.DocumentEntity doc) {
        java.util.List<String> tagList = new java.util.ArrayList<>();
        if (doc.getTags() != null && !doc.getTags().trim().isEmpty()) {
            tagList = java.util.Arrays.asList(doc.getTags().split("\\s*,\\s*"));
        }
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFilename(doc.getOriginalFilename())
                .title(doc.getTitle())
                .tags(tagList)
                .fileSizeBytes(doc.getFileSizeBytes())
                .fileType(doc.getFileType())
                .uploadDate(doc.getUploadDate())
                .extractedEventDate(doc.getExtractedEventDate())
                .processingStatus(doc.getProcessingStatus())
                .category(doc.getCategory())
                .notes(doc.getNotes())
                .build();
    }
}
