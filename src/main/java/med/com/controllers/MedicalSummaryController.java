package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.response.MedicalSummaryResponse;
import med.com.services.MedicalSummaryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class MedicalSummaryController {

    private final MedicalSummaryService medicalSummaryService;

    /**
     * Endpoint: GET /api/v1/patients/medical-summary
     * Purpose: Aggregates all scattered medical data into a single JSON object.
     * Note: For security, we use the authenticated principal instead of taking {patientId}
     * in the path to prevent IDOR (Insecure Direct Object Reference).
     */
    @GetMapping("/medical-summary")
    public ResponseEntity<MedicalSummaryResponse> getMedicalSummary(Principal principal) {
        MedicalSummaryResponse response = medicalSummaryService.getMedicalSummary(principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint: GET /api/v1/patients/medical-summary/pdf
     * Purpose: Generates a PDF of the medical summary and returns it as a binary stream.
     */
    @GetMapping(value = "/medical-summary/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getMedicalSummaryPdf(Principal principal) {
        byte[] pdfBytes = medicalSummaryService.generatePdfSummary(principal.getName());

        HttpHeaders headers = new HttpHeaders();
        // Determine a safe filename based on email or default
        String filename = "Medical_History_" + LocalDate.now().getYear() + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
