package br.com.microservices.orchestrated.paymentservice.core.model.dto;

import br.com.microservices.orchestrated.productvalidationservice.core.model.dto.OrderProductsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    @Id
    private String id;
    private List<OrderProductsDTO> products;
    private LocalDateTime createdAt;
    private String transactionId;
    private double totalAmount;
    private int totalItems;
}
