package br.com.microservices.orchestrated.paymentservice.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductsDTO {

    private ProductDTO product;
    private int quantity;

}
