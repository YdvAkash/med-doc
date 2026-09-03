package med.com.repository;

import med.com.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

        Page<DocumentEntity> findByUserId(Long userId, Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT d FROM DocumentEntity d WHERE d.user.id = :userId " +
                        "AND (:category IS NULL OR :category = '' OR d.category = :category) " +
                        "AND (CAST(:startDate AS timestamp) IS NULL OR d.uploadDate >= :startDate) " +
                        "AND (CAST(:endDate AS timestamp) IS NULL OR d.uploadDate <= :endDate) " +
                        "AND (:search IS NULL OR :search = '' " +
                        "   OR LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "   OR LOWER(d.notes) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "   OR LOWER(d.category) LIKE LOWER(CONCAT('%', :search, '%'))" +
                        "   OR LOWER(d.rawText) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<DocumentEntity> findByUserIdWithFilters(
                        @org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("search") String search,
                        @org.springframework.data.repository.query.Param("category") String category,
                        @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                        @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT d.category, COUNT(d) FROM DocumentEntity d WHERE d.user.id = :userId GROUP BY d.category")
        java.util.List<Object[]> countDocumentsByCategory(
                        @org.springframework.data.repository.query.Param("userId") Long userId);

        Optional<DocumentEntity> findByIdAndUserId(Long id, Long userId);
}
