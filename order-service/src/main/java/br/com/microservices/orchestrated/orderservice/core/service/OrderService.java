package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.kafka.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.model.document.Event;
import br.com.microservices.orchestrated.orderservice.core.model.document.Order;
import br.com.microservices.orchestrated.orderservice.core.model.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.repository.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private static final String TRANSACTIONAL_ID_PATTERN = "%s_%s";

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final EventService eventService;
    private final OrderRepository orderRepository;

    public Order createOrder(OrderRequest orderRequest) {
        var order = Order
                .builder()
                .products(orderRequest.getProducts())
                .createdAt(LocalDateTime.now())
                .transactionId(
                        String.format(TRANSACTIONAL_ID_PATTERN, Instant.now(), UUID.randomUUID())
                )
                .build();

        orderRepository.save(order);
        producer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

    public Event createPayload(Order order) {
        Event event = Event
                .builder()
                .orderId(order.getId())
                .transactionId(order.getTransactionId())
                .payload(order)
                .createdAt(LocalDateTime.now())
                .build();

        return eventService.saveEvent(event);
    }

}
