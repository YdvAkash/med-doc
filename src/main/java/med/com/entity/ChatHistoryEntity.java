package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_history",
    indexes = {
        @Index(name = "idx_chat_user_id", columnList = "user_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "message_type", length = 50)
    private String messageType; // user_question, ai_response

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "related_document_ids", columnDefinition = "TEXT")
    private String relatedDocumentIds; // JSON array

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
