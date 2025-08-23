package br.com.microservices.orchestrated.inventoryservice.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.kafka.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.EventDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.OrderProductsDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.entity.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.entity.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {
    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(EventDTO event)  {
        try  {
            checkCurrentValidation(event);
            createOrderInventory(event);
        } catch (Exception ex) {
            log.error("Error trying to update inventory: ", ex);
//            handleFailCurrentNotExecuted(event, ex.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(EventDTO event) {
        if (orderInventoryRepository.existsByOrderIdAndTransactionId(event.getPayload().getId(),
                event.getTransactionId())) {
            throw new ValidationException("There's another transactionID for the validation");
        }
    }

    private void createOrderInventory(EventDTO event) {
        event
            .getPayload()
            .getProducts()
            .forEach(product -> {
                var inventory = findByProductCode(product.getProductDTO().getCode());
                var orderInventory = createOrderInventory(event, product, inventory);
                orderInventoryRepository.save(orderInventory);

            });
    }

    private OrderInventory createOrderInventory(EventDTO event, OrderProductsDTO product, Inventory inventory) {
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }

    private Inventory findByProductCode(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Product not found in inventory: " + productCode));
    }
}
