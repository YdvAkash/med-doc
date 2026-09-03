package med.com.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_user_id", columnList = "user_id"),
        @Index(name = "idx_documents_extracted_event_date", columnList = "extracted_event_date"),
        @Index(name = "idx_documents_upload_date", columnList = "upload_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "file_s3_path", nullable = false, length = 1000)
    private String fileS3Path;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_type", length = 50)
    private String fileType; // pdf, jpg, png, etc.

    @CreationTimestamp
    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @Column(name = "extracted_event_date")
    private LocalDate extractedEventDate; // The date the document refers to (lab date, X-ray date, etc.)

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText; // OCR extracted text

    @Builder.Default
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Column(name = "processing_status", length = 50)
    private String processingStatus; // pending, processing, completed, failed

    @Column(name = "category", length = 100)
    private String category; // lab, prescription, imaging, vaccination, discharge, etc.

    @Column(name = "title", length = 255)
    private String title; // Smart, AI-extracted readable title

    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated AI-extracted tags

    @Column(name = "provider_name", length = 255)
    private String providerName; // Doctor, clinic, or hospital name

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<ExtractedDataEntity> extractedDataList = new java.util.ArrayList<>();

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AnalysisResultEntity analysisResult;

    @OneToMany(mappedBy = "relatedDocument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<TimelineEventEntity> timelineEvents = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<DocumentChunkEntity> documentChunks = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "relatedDocument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<ReminderEntity> reminders = new java.util.ArrayList<>();

    // New Metadata Fields
    @Column(name = "sample_id", length = 100)
    private String sampleId;

    @Column(name = "ordered_by", length = 255)
    private String orderedBy;

    @Column(name = "verified_status", length = 100)
    private String verifiedStatus; // e.g. "Verified", "Unverified"

    @Column(name = "lab_name", length = 255)
    private String labName;

    @Column(name = "doctor_name", length = 255)
    private String doctorName;

    @ManyToMany(mappedBy = "documents", fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<FolderEntity> folders = new java.util.ArrayList<>();
}
