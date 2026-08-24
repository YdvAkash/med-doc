package med.com.repository;

import med.com.entity.ChatHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistoryEntity, Long> {
    Page<ChatHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<ChatHistoryEntity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
