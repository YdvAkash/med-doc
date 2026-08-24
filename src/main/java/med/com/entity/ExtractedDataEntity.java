package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "extracted_data",
    indexes = {
        @Index(name = "idx_extracted_document_id", columnList = "document_id"),
        @Index(name = "idx_extracted_field_name", columnList = "field_name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(name = "field_name", length = 255)
    private String fieldName; // HbA1c, WBC Count, etc.

    @Column(name = "value", length = 255)
    private String value;

    @Column(name = "unit", length = 100)
    private String unit; // mg/dL, cells/μL, etc.

    @Column(name = "reference_range_min")
    private BigDecimal referenceRangeMin;

    @Column(name = "reference_range_max")
    private BigDecimal referenceRangeMax;

    @Column(name = "reference_range_text", length = 255)
    private String referenceRangeText; // "70-100 mg/dL"

    @Column(name = "is_abnormal")
    private Boolean isAbnormal;

    @Column(name = "abnormality_type", length = 50)
    private String abnormalityType; // high, low, critical

    @Column(name = "data_type", length = 50)
    private String dataType; // numeric, text, date

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
