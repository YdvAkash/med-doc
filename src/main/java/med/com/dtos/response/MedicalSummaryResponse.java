package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MedicalSummaryResponse {
    private String status;
    private MedicalSummaryData data;

    @Data
    @Builder
    public static class MedicalSummaryData {
        private PatientInfoDTO patientInfo;
        private List<MedicationDTO> currentMedications;
        private List<TestResultDTO> testResults;
        private List<ConditionDTO> medicalConditions;
        private List<DocumentSummaryDTO> documentList;
    }

    @Data
    @Builder
    public static class PatientInfoDTO {
        private String fullName;
        private String dob;
        private String bloodGroup;
        private List<String> allergies;
    }

    @Data
    @Builder
    public static class MedicationDTO {
        private String name;
        private String dosage;
        private String frequency;
        private String prescribedDate;
    }

    @Data
    @Builder
    public static class TestResultDTO {
        private String category;
        private String date;
        private List<TestMetricDTO> metrics;
    }

    @Data
    @Builder
    public static class TestMetricDTO {
        private String testName;
        private String value;
        private String unit;
        private String status;
    }

    @Data
    @Builder
    public static class ConditionDTO {
        private String condition;
        private String diagnosedDate;
        private String status;
    }

    @Data
    @Builder
    public static class DocumentSummaryDTO {
        private Long docId;
        private String docType;
        private String dateAdded;
    }
}
