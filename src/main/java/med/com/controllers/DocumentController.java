package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.request.ConfirmDateRequest;
import med.com.dtos.response.ApiResponse;
import med.com.dtos.response.DocumentResponse;
import med.com.dtos.response.DateCandidate;
import med.com.services.DateExtractionService;
import med.com.services.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DateExtractionService dateExtractionService;

    /**
     * POST /api/documents/upload
     * Accepts multipart/form-data with a "file" field.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        DocumentResponse response = documentService.uploadDocument(file, principal.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
    }

    /**
     * GET /api/documents?page=0&size=20&search=keyword&category=Blood Tests
     * Returns paginated list of the authenticated user's documents with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Principal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").descending());
        Page<DocumentResponse> documents = documentService.listDocuments(principal.getName(), search, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * GET /api/documents/{id}
     * Returns a single document with a pre-signed download URL (60 min TTL).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> get(
            @PathVariable Long id,
            Principal principal
    ) {
        DocumentResponse response = documentService.getDocument(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/documents/{id}
     * Deletes the document from S3 and the database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        documentService.deleteDocument(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * GET /api/documents/{id}/text
     * Returns extracted raw text and date candidates
     */
    @GetMapping("/{id}/text")
    public ResponseEntity<ApiResponse<Object>> getText(
            @PathVariable Long id,
            Principal principal
    ) {
        String rawText = documentService.getRawText(id, principal.getName());
        // For now, let's assume category is null as we don't fetch it explicitly here, 
        // or we could fetch the document to pass category. Let's pass general.
        java.util.List<DateCandidate> candidates = dateExtractionService.extractDates(rawText, "general");
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("rawText", rawText);
        response.put("dateCandidates", candidates);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/documents/{id}/confirm-date
     * Confirms the extracted event date
     */
    @PostMapping("/{id}/confirm-date")
    public ResponseEntity<ApiResponse<DocumentResponse>> confirmDate(
            @PathVariable Long id,
            @RequestBody ConfirmDateRequest request,
            Principal principal
    ) {
        DocumentResponse response = documentService.confirmDate(id, request.getExtractedEventDate(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
