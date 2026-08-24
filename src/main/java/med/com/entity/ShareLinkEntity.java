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
    name = "share_links",
    indexes = {
        @Index(name = "idx_share_token", columnList = "token"),
        @Index(name = "idx_share_user_id", columnList = "user_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token", unique = true, nullable = false, length = 255)
    private String token;

    @Column(name = "share_type", length = 100)
    private String shareType; // full_timeline, emergency_card, specific_document

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "accessed_count")
    private Integer accessedCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
}
