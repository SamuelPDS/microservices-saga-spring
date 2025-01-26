package br.com.microservices.orchestrated.productvalidationservice.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductsDTO {

    private ProductDTO productDTO;
    private int quantity;

}
