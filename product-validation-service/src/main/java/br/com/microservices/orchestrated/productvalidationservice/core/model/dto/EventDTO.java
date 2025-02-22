package br.com.microservices.orchestrated.productvalidationservice.core.model.dto;

import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
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
    private List<HistoryDTO> eventHistoryDTO;
    private LocalDateTime createdAt;

    public void addToHistory(HistoryDTO history) {
        if(isEmpty(eventHistoryDTO)) {
            eventHistoryDTO = new ArrayList<>();
        }

        eventHistoryDTO.add(history);
    }
}
