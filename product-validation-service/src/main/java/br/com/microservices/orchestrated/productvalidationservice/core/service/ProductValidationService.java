package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.kafka.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.model.dto.EventDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.model.dto.HistoryDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.model.dto.OrderProductsDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.model.entity.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.SUCCESS;
import static org.springframework.util.ObjectUtils.isEmpty;


@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {
    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(EventDTO event) {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to validade Product: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
    }

    private void validateProductsInformed(EventDTO event) {
        if(isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("Product is empty");
        }

        if(isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderId and Transaction id must be informed!");
        }
    }

    private void checkCurrentValidation(EventDTO event) {
        validateProductsInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(),
            event.getTransactionId())) {
            throw new ValidationException("There's another transactionID for the validation");
        }

        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProductDTO().getCode());
        });
    }

    private void validateProductInformed(OrderProductsDTO product) {
        if (isEmpty(product.getProductDTO()) || isEmpty(product.getProductDTO().getCode())) {
            throw new ValidationException("Product must be informed!");
        }
    }

    private void validateExistingProduct(String code) {
        if (!productRepository.existsByCode(code)) {
            throw new ValidationException("Product does not exists in database!");
        }
    }

    private void createValidation(EventDTO event, boolean success) {
        Validation validation = Validation
                .builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }

    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event);
    }

    private void addHistory(EventDTO event) {
        HistoryDTO history = HistoryDTO
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message("products validated succssfully!")
                .build();

        event.addToHistory(history);
    }
}
