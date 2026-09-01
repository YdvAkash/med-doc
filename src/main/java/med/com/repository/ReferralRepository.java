package med.com.repository;

import med.com.entity.ReferralEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<ReferralEntity, Long> {

    Optional<ReferralEntity> findByReferredUserId(Long referredUserId);

    Page<ReferralEntity> findByReferrerUserIdOrderByCreatedAtDesc(Long referrerUserId, Pageable pageable);

    long countByReferrerUserId(Long referrerUserId);

    long countByReferrerUserIdAndStatus(Long referrerUserId, String status);

    boolean existsByReferredUserId(Long referredUserId);
}
