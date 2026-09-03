package med.com.repository;

import med.com.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    List<FolderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<FolderEntity> findByIdAndUserId(Long id, Long userId);
}
