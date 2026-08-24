package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "document_trends",
    indexes = {
        @Index(name = "idx_trend_user_id", columnList = "user_id"),
        @Index(name = "idx_trend_field_name", columnList = "field_name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTrendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "field_name", length = 255)
    private String fieldName;

    @Column(name = "data_points", columnDefinition = "TEXT")
    private String dataPoints; // JSON array of {date, value}

    @Column(name = "trend_direction", length = 50)
    private String trendDirection; // improving, declining, stable

    @Column(name = "trend_percentage")
    private BigDecimal trendPercentage;

    @Column(name = "first_measurement_date")
    private LocalDate firstMeasurementDate;

    @Column(name = "last_measurement_date")
    private LocalDate lastMeasurementDate;

    @Column(name = "data_point_count")
    private Integer dataPointCount;

    @CreationTimestamp
    @Column(name = "cached_at", nullable = false, updatable = false)
    private LocalDateTime cachedAt;
}
