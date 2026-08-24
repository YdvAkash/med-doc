package med.com.repository;

import med.com.entity.TimelineEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEventEntity, Long> {
    List<TimelineEventEntity> findByUserIdOrderByEventDateDesc(Long userId);
    List<TimelineEventEntity> findByUserIdAndEventTypeOrderByEventDateDesc(Long userId, String eventType);
    Optional<TimelineEventEntity> findByRelatedDocumentId(Long documentId);
}
