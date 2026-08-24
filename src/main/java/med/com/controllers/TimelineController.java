package med.com.controllers;

import lombok.RequiredArgsConstructor;
import med.com.dtos.response.ApiResponse;
import med.com.dtos.response.TimelineEventResponse;
import med.com.entity.TimelineEventEntity;
import med.com.services.TimelineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    /**
     * GET /api/timeline
     * Returns sorted timeline of all events for the user.
     * Supports filtering by date range and event type.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String eventType,
            Principal principal
    ) {
        List<TimelineEventEntity> events = timelineService.getTimeline(principal.getName(), startDate, endDate, eventType);
        
        List<TimelineEventResponse> responseList = events.stream()
                .map(TimelineEventResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseList));
    }
}
