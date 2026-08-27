package med.com.repository;

import med.com.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmailAndOtpAndPurpose(String email, String otp, String purpose);
    void deleteByEmailAndPurpose(String email, String purpose);
}
