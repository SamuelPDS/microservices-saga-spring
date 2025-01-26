package br.com.microservices.orchestrated.paymentservice.core.model.dto;

import br.com.microservices.orchestrated.productvalidationservice.core.model.dto.ProductDTO;
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
