package med.com.dtos.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class profileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String bloodGroup;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String chronicConditions;
    private String allergies;
    private LocalDate dateOfBirth;
    private String profilePictureUrl;
}
