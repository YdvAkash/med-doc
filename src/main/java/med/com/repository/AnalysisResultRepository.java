package med.com.repository;

import med.com.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResultEntity, Long> {
    Optional<AnalysisResultEntity> findByDocumentId(Long documentId);
}
