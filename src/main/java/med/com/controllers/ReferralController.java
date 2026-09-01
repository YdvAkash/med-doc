package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.response.ReferralProgressResponse;
import med.com.dtos.response.ReferralStatsResponse;
import med.com.services.ReferralService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/me")
    public ResponseEntity<ReferralStatsResponse> getReferralStats(Authentication authentication) {
        String email = authentication.getName();
        ReferralStatsResponse stats = referralService.getReferralStats(email);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<ReferralProgressResponse>> getReferralHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String email = authentication.getName();
        Page<ReferralProgressResponse> history = referralService.getReferralHistory(email, page, size);
        return ResponseEntity.ok(history);
    }
}
