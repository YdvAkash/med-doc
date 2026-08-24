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
@Table(name = "analysis_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private DocumentEntity document;

    @Column(name = "analysis_type", length = 50)
    private String analysisType; // single_document, trend, comparative

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_findings", columnDefinition = "TEXT")
    private String keyFindings; // JSON array

    @Column(columnDefinition = "TEXT")
    private String abnormalities; // JSON array

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "full_analysis", columnDefinition = "TEXT")
    private String fullAnalysis; // Complete response

    @Column(name = "analysis_tokens_used")
    private Integer analysisTokensUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
