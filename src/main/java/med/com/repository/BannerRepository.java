package med.com.repository;

import med.com.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<BannerEntity, Long> {
    List<BannerEntity> findByIsActiveTrueOrderByCreatedAtDesc();
}
