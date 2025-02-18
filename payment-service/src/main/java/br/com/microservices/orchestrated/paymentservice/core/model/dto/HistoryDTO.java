package br.com.microservices.orchestrated.paymentservice.core.model.dto;

import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryDTO {

    private String source;
    private ESagaStatus status;
    private String message;
    private LocalDateTime createdAt;
}
