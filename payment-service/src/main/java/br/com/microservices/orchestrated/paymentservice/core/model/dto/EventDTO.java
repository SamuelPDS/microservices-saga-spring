package br.com.microservices.orchestrated.paymentservice.core.model.dto;


import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private String id;
    private String transactionId;
    private String orderId;
    private OrderDTO payload;
    private String source;
    private ESagaStatus status;
    private List<HistoryDTO> eventHistory;
    private LocalDateTime createdAt;

   public void addToHistory(HistoryDTO history) {
        if(isEmpty(eventHistory)) {
            eventHistory = new ArrayList<>();
        }

        eventHistory.add(history);
    }
}
