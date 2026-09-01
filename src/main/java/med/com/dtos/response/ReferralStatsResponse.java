package med.com.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferralStatsResponse {
    private String referralCode;
    private String referralLink;
    private long totalReferrals;
    private long successfulReferrals;
    private long pendingReferrals;
    private long rewardedReferrals;
    private int totalCreditsEarned;
}
