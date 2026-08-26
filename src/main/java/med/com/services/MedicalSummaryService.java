package med.com.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.dtos.response.MedicalSummaryResponse;
import med.com.dtos.response.MedicalSummaryResponse.*;
import med.com.entity.*;
import med.com.repository.DocumentRepository;
import med.com.repository.TimelineEventRepository;
import med.com.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalSummaryService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final TemplateEngine templateEngine;

    public MedicalSummaryResponse getMedicalSummary(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Patient Info
        PatientInfoDTO patientInfo = PatientInfoDTO.builder()
                .fullName(user.getFirstName() + " " + user.getLastName())
                .dob(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "N/A")
                .bloodGroup(user.getBloodGroup() != null ? user.getBloodGroup() : "Unknown")
                .allergies(parseCommaSeparated(user.getAllergies()))
                .build();

        // 2. Medical Conditions (parsing from chronicConditions)
        List<ConditionDTO> conditions = new ArrayList<>();
        if (user.getChronicConditions() != null && !user.getChronicConditions().isBlank()) {
            String[] parts = user.getChronicConditions().split(",");
            for (String part : parts) {
                conditions.add(ConditionDTO.builder()
                        .condition(part.trim())
                        .diagnosedDate("Unknown") // Not stored in DB
                        .status("Active")
                        .build());
            }
        }

        // 3. Medications (parsing from Timeline Events where type = prescription)
        List<TimelineEventEntity> timelineEvents = timelineEventRepository.findByUserIdOrderByEventDateDesc(user.getId());
        List<MedicationDTO> medications = new ArrayList<>();
        for (TimelineEventEntity event : timelineEvents) {
            if ("prescription".equalsIgnoreCase(event.getEventType())) {
                medications.add(MedicationDTO.builder()
                        .name(event.getTitle() != null ? event.getTitle() : "Unknown Medication")
                        .dosage("Check document")
                        .frequency("Check document")
                        .prescribedDate(event.getEventDate().toString())
                        .build());
            }
        }

        // 4. Test Results and Documents
        List<DocumentEntity> documents = documentRepository.findByUserId(user.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<TestResultDTO> testResults = new ArrayList<>();
        List<DocumentSummaryDTO> documentList = new ArrayList<>();

        for (DocumentEntity doc : documents) {
            // Document Summary
            documentList.add(DocumentSummaryDTO.builder()
                    .docId(doc.getId())
                    .docType(doc.getCategory() != null ? doc.getCategory() : "General")
                    .dateAdded(doc.getUploadDate() != null ? doc.getUploadDate().toLocalDate().toString() : "Unknown")
                    .build());

            // Test Results (if document has extracted data)
            if (doc.getExtractedDataList() != null && !doc.getExtractedDataList().isEmpty()) {
                List<TestMetricDTO> metrics = new ArrayList<>();
                for (ExtractedDataEntity data : doc.getExtractedDataList()) {
                    metrics.add(TestMetricDTO.builder()
                            .testName(data.getFieldName())
                            .value(data.getValue())
                            .unit(data.getUnit() != null ? data.getUnit() : "")
                            .status(Boolean.TRUE.equals(data.getIsAbnormal()) ? "Abnormal" : "Normal")
                            .build());
                }
                
                testResults.add(TestResultDTO.builder()
                        .category(doc.getCategory() != null ? doc.getCategory() : "Test Result")
                        .date(doc.getExtractedEventDate() != null ? doc.getExtractedEventDate().toString() : 
                                (doc.getUploadDate() != null ? doc.getUploadDate().toLocalDate().toString() : "Unknown"))
                        .metrics(metrics)
                        .build());
            }
        }

        MedicalSummaryData data = MedicalSummaryData.builder()
                .patientInfo(patientInfo)
                .currentMedications(medications)
                .testResults(testResults)
                .medicalConditions(conditions)
                .documentList(documentList)
                .build();

        return MedicalSummaryResponse.builder()
                .status("success")
                .data(data)
                .build();
    }

    public byte[] generatePdfSummary(String email) {
        try {
            MedicalSummaryResponse response = getMedicalSummary(email);
            MedicalSummaryData data = response.getData();

            Context context = new Context();
            context.setVariable("data", data);

            String html = templateEngine.process("medical_summary_template", context);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }
    }

    private List<String> parseCommaSeparated(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
