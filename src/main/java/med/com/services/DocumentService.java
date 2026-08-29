package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.dtos.response.DocumentResponse;
import med.com.dtos.response.DateCandidate;
import med.com.entity.DocumentEntity;
import med.com.entity.UserEntity;
import med.com.exceptions.BadRequestException;
import med.com.exceptions.ResourceNotFoundException;
import med.com.repository.DocumentRepository;
import med.com.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    private final GeminiService geminiService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Autowired
    @Lazy
    private DocumentService self;

    // -----------------------------------------------------------------------
    // Upload
    // -----------------------------------------------------------------------
    public DocumentResponse uploadDocument(MultipartFile file, String email) {
        validateFile(file);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

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
        self.processDocumentAsync(saved.getId());

        return toResponse(saved, null);
    }

    // -----------------------------------------------------------------------
    // List (paginated with filters)
    // -----------------------------------------------------------------------
    public Page<DocumentResponse> listDocuments(String email, String search, String category, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        return documentRepository.findByUserIdWithFilters(user.getId(), search, category, pageable)
                .map(doc -> toResponse(doc, null));
    }

    // -----------------------------------------------------------------------
    // Get single (with presigned download URL)
    // -----------------------------------------------------------------------
    public DocumentResponse getDocument(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

        String presignedUrl = s3Service.getPresignedUrl(doc.getFileS3Path());
        return toResponse(doc, presignedUrl);
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------
    public void deleteDocument(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

        s3Service.deleteFile(doc.getFileS3Path());
        documentRepository.delete(doc);
        log.info("Document id={} deleted for user={}", documentId, email);
    }

    // -----------------------------------------------------------------------
    // Get Raw Text
    // -----------------------------------------------------------------------
    public String getRawText(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

        return doc.getRawText();
    }

    // -----------------------------------------------------------------------
    // Confirm Date and Create Timeline Event
    // -----------------------------------------------------------------------
    public DocumentResponse confirmDate(Long documentId, LocalDate extractedEventDate, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

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
            throw new BadRequestException("FILE_EMPTY", "File cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("FILE_TOO_LARGE", "File size exceeds the 10MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("INVALID_FILE_TYPE", "Invalid file type. Only PDF, JPG, and PNG are allowed.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocumentResponse toResponse(DocumentEntity doc, String downloadUrl) {
        java.util.List<DocumentResponse.MetricDto> metrics = new java.util.ArrayList<>();
        if (doc.getExtractedDataList() != null) {
            for (med.com.entity.ExtractedDataEntity data : doc.getExtractedDataList()) {
                DocumentResponse.MetricDto metric = new DocumentResponse.MetricDto();
                metric.setName(data.getFieldName());
                metric.setValue(data.getValue());
                metric.setUnit(data.getUnit());
                metric.setStatus(data.getStatus());
                metric.setIcon(data.getIcon());
                metrics.add(metric);
            }
        }

        java.util.List<String> tagList = new java.util.ArrayList<>();
        if (doc.getTags() != null && !doc.getTags().trim().isEmpty()) {
            tagList = java.util.Arrays.asList(doc.getTags().split("\\s*,\\s*"));
        }
        
        String summary = null;
        if (doc.getAnalysisResult() != null) {
            summary = doc.getAnalysisResult().getSummary();
        }

        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFilename(doc.getOriginalFilename())
                .title(doc.getTitle())
                .tags(tagList)
                .fileSizeBytes(doc.getFileSizeBytes())
                .fileType(doc.getFileType())
                .uploadDate(doc.getUploadDate())
                .extractedEventDate(doc.getExtractedEventDate())
                .processingStatus(doc.getProcessingStatus())
                .category(doc.getCategory())
                .notes(doc.getNotes())
                .downloadUrl(downloadUrl)
                .summary(summary)
                .metrics(metrics)
                .build();
    }

    // -----------------------------------------------------------------------
    // AI Summary Generation
    // -----------------------------------------------------------------------
    public String generateDoctorSummary(Long documentId, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

        if (doc.getRawText() == null || doc.getRawText().trim().isEmpty()) {
            throw new BadRequestException("NO_TEXT", "Document does not have extracted text to summarize yet.");
        }

        String summary = geminiService.generateSummary(doc.getRawText());

        med.com.entity.AnalysisResultEntity analysisResult = doc.getAnalysisResult();
        if (analysisResult == null) {
            analysisResult = med.com.entity.AnalysisResultEntity.builder()
                    .document(doc)
                    .analysisType("single_document")
                    .build();
        }
        
        analysisResult.setSummary(summary);
        doc.setAnalysisResult(analysisResult);
        documentRepository.save(doc); // Cascades save to AnalysisResultEntity

        return summary;
    }

    public DocumentResponse translateDocument(Long documentId, String language, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));

        DocumentResponse response = toResponse(doc, null); // Re-use the existing logic to build the base DTO
        
        // Translate Summary
        if (response.getSummary() != null && !response.getSummary().isEmpty()) {
            String translatedSummary = geminiService.translateText(response.getSummary(), language);
            response.setSummary(translatedSummary);
        }

        // Translate Metric names and statuses
        if (response.getMetrics() != null && !response.getMetrics().isEmpty()) {
            for (DocumentResponse.MetricDto metric : response.getMetrics()) {
                if (metric.getName() != null) {
                    metric.setName(geminiService.translateText(metric.getName(), language));
                }
                if (metric.getStatus() != null && !metric.getStatus().isEmpty()) {
                    metric.setStatus(geminiService.translateText(metric.getStatus(), language));
                }
            }
        }

        return response;
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

            if (extractedText == null || extractedText.trim().length() < 10) {
                log.warn("Document {} rejected as no meaningful text was found.", documentId);
                doc.setProcessingStatus("failed");
                doc.setNotes("No text was found in the uploaded image. Please ensure the document is clear and readable.");
                documentRepository.save(doc);
                return;
            }

            doc.setRawText(extractedText);
            
            // Automatically extract date
            List<DateCandidate> dates = dateExtractionService.extractDates(extractedText, doc.getCategory());
            if (!dates.isEmpty()) {
                doc.setExtractedEventDate(dates.get(0).getDate());
            }

            // Extract metadata (title, tags) and metrics using Gemini API
            GeminiService.DocumentAnalysisResult analysisResult = geminiService.extractDocumentMetadata(extractedText);
            
            if (analysisResult != null) {
                if ("Unknown Document".equalsIgnoreCase(analysisResult.getTitle())) {
                    log.warn("Document {} rejected by AI as it does not appear to be a medical report.", documentId);
                    doc.setProcessingStatus("failed");
                    doc.setNotes("The uploaded image does not appear to be a valid medical report. Please upload a clear photo of your document.");
                    documentRepository.save(doc);
                    return;
                }
                
                if (analysisResult.getTitle() != null) {
                    doc.setTitle(analysisResult.getTitle());
                }
                if (analysisResult.getTags() != null && !analysisResult.getTags().isEmpty()) {
                    doc.setTags(String.join(", ", analysisResult.getTags()));
                }
                
                java.util.List<DocumentResponse.MetricDto> metricsDtoList = analysisResult.getMetrics();
                if (metricsDtoList != null && !metricsDtoList.isEmpty()) {
                    java.util.List<med.com.entity.ExtractedDataEntity> extractedDataList = new java.util.ArrayList<>();
                    for (DocumentResponse.MetricDto dto : metricsDtoList) {
                        med.com.entity.ExtractedDataEntity data = med.com.entity.ExtractedDataEntity.builder()
                                .document(doc)
                                .fieldName(dto.getName())
                                .value(dto.getValue())
                                .unit(dto.getUnit())
                                .status(dto.getStatus())
                                .icon(dto.getIcon())
                                .build();
                        extractedDataList.add(data);
                    }
                    doc.setExtractedDataList(extractedDataList);
                }
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
