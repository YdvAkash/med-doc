package med.com.services;

import lombok.extern.slf4j.Slf4j;
import med.com.dtos.response.DateCandidate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DateExtractionService {

    // Regex patterns for various date formats
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
            // yyyy-MM-dd or yyyy/MM/dd
            Pattern.compile("\\b(20\\d{2})[-/](0[1-9]|1[0-2])[-/](0[1-9]|[12]\\d|3[01])\\b"),
            // dd-MM-yyyy or dd/MM/yyyy
            Pattern.compile("\\b(0[1-9]|[12]\\d|3[01])[-/](0[1-9]|1[0-2])[-/](20\\d{2})\\b"),
            // dd MMM yyyy or dd-MMM-yyyy (e.g., 15 Jan 2023)
            Pattern.compile("\\b(0[1-9]|[12]\\d|3[01])[\\s-](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[\\s-](20\\d{2})\\b", Pattern.CASE_INSENSITIVE)
    );

    // Keywords that indicate a date might be highly relevant (e.g., "Date:", "Report Date:")
    private static final List<String> HIGH_CONFIDENCE_KEYWORDS = Arrays.asList(
            "date:", "date", "report date:", "collected:", "drawn:", "test date:", "prescribed:"
    );

    public List<DateCandidate> extractDates(String text, String category) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Map<LocalDate, Double> candidateMap = new HashMap<>();
        String lowerText = text.toLowerCase();

        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String dateStr = matcher.group();
                LocalDate parsedDate = parseDate(dateStr);
                
                if (parsedDate != null && !parsedDate.isAfter(LocalDate.now())) {
                    // Calculate confidence based on proximity to keywords
                    double confidence = calculateConfidence(lowerText, matcher.start());
                    
                    // If date already found, keep the highest confidence
                    candidateMap.merge(parsedDate, confidence, Math::max);
                }
            }
        }

        List<DateCandidate> candidates = new ArrayList<>();
        candidateMap.forEach((date, confidence) -> candidates.add(new DateCandidate(date, confidence)));
        
        // Sort by confidence descending
        candidates.sort((c1, c2) -> Double.compare(c2.getConfidence(), c1.getConfidence()));

        return candidates;
    }

    private LocalDate parseDate(String dateStr) {
        // Try common formats
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                // Capitalize first letter of month for MMM patterns if it's lowercase
                String toParse = dateStr;
                if (dateStr.matches(".*[a-zA-Z].*")) {
                     String[] parts = dateStr.split("[\\s-]");
                     if (parts.length == 3) {
                         String month = parts[1];
                         parts[1] = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();
                         toParse = String.join(dateStr.contains("-") ? "-" : " ", parts);
                     }
                }
                return LocalDate.parse(toParse, formatter);
            } catch (DateTimeParseException e) {
                // Ignore and try next formatter
            }
        }
        log.warn("Could not parse date string: {}", dateStr);
        return null;
    }

    private double calculateConfidence(String text, int matchStartIndex) {
        // Look backwards up to 30 characters to find keywords
        int startSearch = Math.max(0, matchStartIndex - 30);
        String precedingText = text.substring(startSearch, matchStartIndex);
        
        for (String keyword : HIGH_CONFIDENCE_KEYWORDS) {
            if (precedingText.contains(keyword)) {
                return 0.9; // High confidence
            }
        }
        
        return 0.5; // Default confidence
    }
}
