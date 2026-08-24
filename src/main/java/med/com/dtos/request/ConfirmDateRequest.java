package med.com.dtos.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConfirmDateRequest {
    private LocalDate extractedEventDate;
}
