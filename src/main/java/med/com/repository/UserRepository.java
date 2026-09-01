package med.com.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import med.com.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmail(String email);
    Optional<UserEntity> findByEmail(String email); 
    java.util.List<UserEntity> findByPushTokenIsNotNull();
    Optional<UserEntity> findByMyReferralCode(String myReferralCode);
}
