package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String subscriptionTier;
    private String myReferralCode;
    private Integer credits;
}
