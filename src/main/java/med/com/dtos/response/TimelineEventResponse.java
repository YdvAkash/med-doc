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

    public static TimelineEventResponse fromEntity(TimelineEventEntity entity) {
        return TimelineEventResponse.builder()
                .id(entity.getId())
                .eventDate(entity.getEventDate())
                .eventType(entity.getEventType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .severity(entity.getSeverity())
                .relatedDocumentId(entity.getRelatedDocument() != null ? entity.getRelatedDocument().getId() : null)
                .build();
    }
}
