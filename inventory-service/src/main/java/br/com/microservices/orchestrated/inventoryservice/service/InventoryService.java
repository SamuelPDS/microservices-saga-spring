package br.com.microservices.orchestrated.inventoryservice.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.kafka.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.EventDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.HistoryDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.OrderDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.dto.OrderProductsDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.entity.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.entity.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.*;

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
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to update inventory: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void updateInventory(OrderDTO order) {
        order
            .getProducts()
            .forEach(product -> {
                var inventory = findByProductCode(product.getProduct().getCode());
                checkInventory(inventory.getAvailable(), product.getQuantity());
                inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                inventoryRepository.save(inventory);
            });
    }

    private void checkInventory(int avaliable, int orderQuantity) {
        if (avaliable < orderQuantity) {
            throw new ValidationException("Insufficient inventory for the product");
        }
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
                var inventory = findByProductCode(product.getProduct().getCode());
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
                .orElseThrow(() -> new ValidationException("Product not founded in inventory: " + productCode));
    }


    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory successfully updated!");
    }

    private void addHistory(EventDTO event, String messsage) {
        HistoryDTO history = HistoryDTO
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(messsage)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(EventDTO event, String errorMessage) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory: ".concat(errorMessage));
    }

    public void rollbackInventory(EventDTO event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            returnInventoryToPreviousValue(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception e) {
            addHistory(event, "Rollback not excuted for inventory: ".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void returnInventoryToPreviousValue(EventDTO event) {
        orderInventoryRepository
                .findByOrderIdAndTransactionId(event.getOrderId(),
                event.getTransactionId())
                .forEach(order -> {
                    var inventory = order.getInventory();
                    inventory.setAvailable(order.getOldQuantity());
                    inventoryRepository.save(inventory);
                 log.info("Restored inventory for order {} from {} to {} ", event.getPayload().getId(), order.getNewQuantity(), inventory.getAvailable());
                });
    }
}
