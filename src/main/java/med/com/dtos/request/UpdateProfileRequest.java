package med.com.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String bloodGroup;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String chronicConditions;
    private String allergies;
    private LocalDate dateOfBirth;
    private String profilePictureUrl;
}
