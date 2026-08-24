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
    name = "reminders",
    indexes = {
        @Index(name = "idx_reminder_user_id", columnList = "user_id"),
        @Index(name = "idx_reminder_due_date", columnList = "due_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "reminder_type", length = 100)
    private String reminderType; // medication, follow_up_test, appointment

    @Column(name = "title", length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "frequency", length = 50)
    private String frequency; // one_time, daily, weekly, monthly

    @Column(name = "status", length = 50)
    private String status; // pending, completed, dismissed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_document_id")
    private DocumentEntity relatedDocument;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
