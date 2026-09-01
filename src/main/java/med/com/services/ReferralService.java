package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.entity.ReferralEntity;
import med.com.entity.UserEntity;
import med.com.repository.ReferralRepository;
import med.com.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;

    public String generateUniqueReferralCode(String name) {
        String base = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "MED";
        String code;
        do {
            code = base + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        } while (userRepository.findByMyReferralCode(code).isPresent());
        return code;
    }

    @Transactional
    public void processReferralOnSignup(UserEntity referredUser, String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        // Self-referral check (just in case, though on signup it's a new user)
        if (referralCode.equalsIgnoreCase(referredUser.getMyReferralCode())) {
            log.warn("Self-referral attempt by {}", referredUser.getEmail());
            return;
        }

        Optional<UserEntity> referrerOpt = userRepository.findByMyReferralCode(referralCode.toUpperCase());
        if (referrerOpt.isEmpty()) {
            log.warn("Invalid referral code used during signup: {}", referralCode);
            return;
        }

        UserEntity referrer = referrerOpt.get();

        // Check if a referral already exists
        if (referralRepository.existsByReferredUserId(referredUser.getId())) {
            log.warn("Referral already exists for referred user {}", referredUser.getEmail());
            return;
        }

        ReferralEntity referral = ReferralEntity.builder()
                .referrerUser(referrer)
                .referredUser(referredUser)
                .referralCode(referralCode.toUpperCase())
                .status("PENDING")
                .documentsCompleted(0)
                .chatsCompleted(0)
                .build();

        referralRepository.save(referral);
        log.info("Created PENDING referral for referred user {} with referrer {}", referredUser.getEmail(), referrer.getEmail());
    }

    @Transactional
    public void incrementDocumentCount(Long referredUserId) {
        referralRepository.findByReferredUserId(referredUserId).ifPresent(referral -> {
            if ("PENDING".equals(referral.getStatus())) {
                referral.setDocumentsCompleted(referral.getDocumentsCompleted() + 1);
                referralRepository.save(referral);
                checkAndReward(referral);
            }
        });
    }

    @Transactional
    public void incrementChatCount(Long referredUserId) {
        referralRepository.findByReferredUserId(referredUserId).ifPresent(referral -> {
            if ("PENDING".equals(referral.getStatus())) {
                referral.setChatsCompleted(referral.getChatsCompleted() + 1);
                referralRepository.save(referral);
                checkAndReward(referral);
            }
        });
    }

    @Transactional
    public void checkAndReward(ReferralEntity referral) {
        // Double check status inside transactional method
        if (!"PENDING".equals(referral.getStatus())) {
            return;
        }

        if (referral.getDocumentsCompleted() >= 2 && referral.getChatsCompleted() >= 5) {
            referral.setStatus("REWARDED");
            referral.setQualifiedAt(LocalDateTime.now());
            referral.setRewardGrantedAt(LocalDateTime.now());
            
            // Add credits to referrer
            UserEntity referrer = referral.getReferrerUser();
            referrer.setCredits(referrer.getCredits() + 50); // 50 credits reward
            userRepository.save(referrer);

            referralRepository.save(referral);
            log.info("Referral REWARDED for referrer {}, referred {}", referrer.getEmail(), referral.getReferredUser().getEmail());
        }
    }

    public med.com.dtos.response.ReferralStatsResponse getReferralStats(String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        
        long total = referralRepository.countByReferrerUserId(user.getId());
        long pending = referralRepository.countByReferrerUserIdAndStatus(user.getId(), "PENDING");
        long rewarded = referralRepository.countByReferrerUserIdAndStatus(user.getId(), "REWARDED");

        return med.com.dtos.response.ReferralStatsResponse.builder()
                .referralCode(user.getMyReferralCode())
                .referralLink("https://mediva.ogakash.xyz")
                .totalReferrals(total)
                .successfulReferrals(rewarded)
                .pendingReferrals(pending)
                .rewardedReferrals(rewarded)
                .totalCreditsEarned(user.getCredits() != null ? user.getCredits() : 0)
                .build();
    }

    public org.springframework.data.domain.Page<med.com.dtos.response.ReferralProgressResponse> getReferralHistory(String email, int page, int size) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        return referralRepository.findByReferrerUserIdOrderByCreatedAtDesc(user.getId(), org.springframework.data.domain.PageRequest.of(page, size))
                .map(ref -> med.com.dtos.response.ReferralProgressResponse.builder()
                        .referredUserName(ref.getReferredUser().getFirstName() + " " + ref.getReferredUser().getLastName())
                        .status(ref.getStatus())
                        .documentsCompleted(ref.getDocumentsCompleted())
                        .chatsCompleted(ref.getChatsCompleted())
                        .createdAt(ref.getCreatedAt())
                        .qualifiedAt(ref.getQualifiedAt())
                        .build());
    }
}
