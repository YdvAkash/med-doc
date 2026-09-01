package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "referrals",
    indexes = {
        @Index(name = "idx_referral_code", columnList = "referral_code"),
        @Index(name = "idx_referrer_id", columnList = "referrer_user_id"),
        @Index(name = "idx_referred_id", columnList = "referred_user_id", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private UserEntity referrerUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_user_id", nullable = false, unique = true)
    private UserEntity referredUser;

    @Column(name = "referral_code", nullable = false, length = 50)
    private String referralCode;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, QUALIFIED, REWARDED

    @Column(name = "documents_completed", nullable = false)
    @Builder.Default
    private Integer documentsCompleted = 0;

    @Column(name = "chats_completed", nullable = false)
    @Builder.Default
    private Integer chatsCompleted = 0;

    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    @Column(name = "reward_granted_at")
    private LocalDateTime rewardGrantedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
