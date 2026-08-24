package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.dtos.response.DocumentResponse;
import med.com.dtos.response.DateCandidate;
import med.com.entity.DocumentEntity;
import med.com.entity.UserEntity;
import med.com.repository.DocumentRepository;
import med.com.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final TextractService textractService;
    private final TimelineService timelineService;
    private final DateExtractionService dateExtractionService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    // -----------------------------------------------------------------------
    // Upload
    // -----------------------------------------------------------------------
    public DocumentResponse uploadDocument(MultipartFile file, String email) {
        validateFile(file);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate a unique S3 key: users/{userId}/docs/{uuid}_{originalFilename}
        String extension = getExtension(file.getOriginalFilename());
        String s3Key = "users/" + user.getId() + "/docs/" + UUID.randomUUID() + "." + extension;

        s3Service.uploadFile(file, s3Key);

        DocumentEntity document = DocumentEntity.builder()
                .user(user)
                .originalFilename(file.getOriginalFilename())
                .fileS3Path(s3Key)
                .fileSizeBytes(file.getSize())
                .fileType(extension)
                .processingStatus("pending")
                .isProcessed(false)
                .build();

        DocumentEntity saved = documentRepository.save(document);
        log.info("Document saved to DB with id={} for user={}", saved.getId(), email);

        // Trigger asynchronous OCR processing
        processDocumentAsync(saved.getId());

        return toResponse(saved, null);
    }

    // -----------------------------------------------------------------------
    // List (paginated with filters)
    // -----------------------------------------------------------------------
    public Page<DocumentResponse> listDocuments(String email, String search, String category, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return documentRepository.findByUserIdWithFilters(user.getId(), search, category, pageable)
                .map(doc -> toResponse(doc, null));
    }

    // -----------------------------------------------------------------------
    // Get single (with presigned download URL)
    // -----------------------------------------------------------------------
    public DocumentResponse getDocument(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String presignedUrl = s3Service.getPresignedUrl(doc.getFileS3Path());
        return toResponse(doc, presignedUrl);
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------
    public void deleteDocument(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        s3Service.deleteFile(doc.getFileS3Path());
        documentRepository.delete(doc);
        log.info("Document id={} deleted for user={}", documentId, email);
    }

    // -----------------------------------------------------------------------
    // Get Raw Text
    // -----------------------------------------------------------------------
    public String getRawText(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return doc.getRawText();
    }

    // -----------------------------------------------------------------------
    // Confirm Date and Create Timeline Event
    // -----------------------------------------------------------------------
    public DocumentResponse confirmDate(Long documentId, LocalDate extractedEventDate, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        doc.setExtractedEventDate(extractedEventDate);
        DocumentEntity savedDoc = documentRepository.save(doc);

        // Create the timeline event
        timelineService.createOrUpdateEventFromDocument(savedDoc);

        return toResponse(savedDoc, null);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("File size exceeds the 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Invalid file type. Only PDF, JPG, and PNG are allowed");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocumentResponse toResponse(DocumentEntity doc, String downloadUrl) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFilename(doc.getOriginalFilename())
                .fileSizeBytes(doc.getFileSizeBytes())
                .fileType(doc.getFileType())
                .uploadDate(doc.getUploadDate())
                .extractedEventDate(doc.getExtractedEventDate())
                .processingStatus(doc.getProcessingStatus())
                .category(doc.getCategory())
                .notes(doc.getNotes())
                .downloadUrl(downloadUrl)
                .build();
    }

    // -----------------------------------------------------------------------
    // Async OCR Processing
    // -----------------------------------------------------------------------
    @Async
    public void processDocumentAsync(Long documentId) {
        log.info("Starting asynchronous OCR processing for documentId={}", documentId);
        
        DocumentEntity doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.error("Document id={} not found for OCR processing", documentId);
            return;
        }

        try {
            doc.setProcessingStatus("processing");
            documentRepository.save(doc);

            String extractedText = textractService.extractTextFromS3(bucketName, doc.getFileS3Path());

            doc.setRawText(extractedText);
            
            // Automatically extract date
            List<DateCandidate> dates = dateExtractionService.extractDates(extractedText, doc.getCategory());
            if (!dates.isEmpty()) {
                doc.setExtractedEventDate(dates.get(0).getDate());
            }

            doc.setIsProcessed(true);
            doc.setProcessingStatus("completed");
            documentRepository.save(doc);

            // Automatically create timeline event
            timelineService.createOrUpdateEventFromDocument(doc);

            log.info("Successfully processed OCR and generated timeline event for documentId={}", documentId);
        } catch (Exception e) {
            log.error("Failed to process OCR for documentId={}", documentId, e);
            doc.setProcessingStatus("failed");
            doc.setNotes(e.getMessage());
            documentRepository.save(doc);
        }
    }
}
