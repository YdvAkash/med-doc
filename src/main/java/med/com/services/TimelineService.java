package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.com.entity.DocumentEntity;
import med.com.entity.TimelineEventEntity;
import med.com.entity.UserEntity;
import med.com.repository.TimelineEventRepository;
import med.com.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final UserRepository userRepository;

    public TimelineEventEntity createOrUpdateEventFromDocument(DocumentEntity document) {
        String eventType = inferEventType(document.getCategory());
        String title = "Document Uploaded: " + document.getOriginalFilename();
        if (document.getCategory() != null) {
            title = document.getCategory() + " Document";
        }

        TimelineEventEntity event = timelineEventRepository.findByRelatedDocumentId(document.getId())
                .orElse(new TimelineEventEntity());

        event.setUser(document.getUser());
        event.setEventDate(document.getExtractedEventDate() != null ? document.getExtractedEventDate() : LocalDate.now());
        event.setEventType(eventType);
        event.setRelatedDocument(document);
        event.setTitle(title);
        event.setDescription(document.getNotes());
        
        if (event.getSeverity() == null) {
            event.setSeverity("normal");
        }

        log.info("Saving timeline event for user={} with date={}", document.getUser().getEmail(), event.getEventDate());
        return timelineEventRepository.save(event);
    }

    public List<TimelineEventEntity> getTimeline(String email, LocalDate startDate, LocalDate endDate, String eventType) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Simplistic approach for now: grab all, sort, and filter
        // A better approach would be to use Spring Data JPA Specifications
        List<TimelineEventEntity> events;
        if (eventType != null && !eventType.isEmpty()) {
            events = timelineEventRepository.findByUserIdAndEventTypeOrderByEventDateDesc(user.getId(), eventType);
        } else {
            events = timelineEventRepository.findByUserIdOrderByEventDateDesc(user.getId());
        }

        // Apply date filters in memory for simplicity (can be optimized to DB query)
        return events.stream()
                .filter(e -> (startDate == null || !e.getEventDate().isBefore(startDate)) &&
                             (endDate == null || !e.getEventDate().isAfter(endDate)))
                .toList();
    }

    private String inferEventType(String category) {
        if (category == null) return "general";
        category = category.toLowerCase();
        if (category.contains("lab")) return "lab_result";
        if (category.contains("prescription") || category.contains("rx")) return "prescription";
        if (category.contains("imaging") || category.contains("xray") || category.contains("mri") || category.contains("scan")) return "imaging";
        if (category.contains("vaccine") || category.contains("immunization")) return "vaccination";
        return "general";
    }
}
