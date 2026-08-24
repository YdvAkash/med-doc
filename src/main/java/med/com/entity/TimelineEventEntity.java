package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "timeline_events",
    indexes = {
        @Index(name = "idx_timeline_user_id", columnList = "user_id"),
        @Index(name = "idx_timeline_event_date", columnList = "event_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_type", length = 100)
    private String eventType; // lab_result, prescription, imaging, vaccination

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_document_id")
    private DocumentEntity relatedDocument;

    @Column(name = "title", length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "severity", length = 50)
    private String severity; // normal, warning, critical

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
