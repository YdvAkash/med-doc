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
    name = "document_chunks",
    indexes = {
        @Index(name = "idx_chunk_doc_id", columnList = "document_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    // We will not map this field directly to Java code to avoid hibernate-vector dependency issues.
    // Instead we will manage the vector operations using Native Queries in the Repository.
    // So we don't declare the embedding field here, or we can declare it as transient.
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
