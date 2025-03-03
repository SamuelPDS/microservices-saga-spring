package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.kafka.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.model.dto.EventDTO;
import br.com.microservices.orchestrated.paymentservice.core.model.dto.HistoryDTO;
import br.com.microservices.orchestrated.paymentservice.core.model.dto.OrderProductsDTO;
import br.com.microservices.orchestrated.paymentservice.core.model.entity.Payment;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus.REFUND;
import static br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus.SUCESS;
import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {
    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;

    private final PaymentRepository paymentRepository;
    private final KafkaProducer producer;
    private final JsonUtil jsonUtil;

    public void realizePayment(EventDTO event) {
        try{
            checkCurrentValidation(event);
            createPendingPayment(event);
            Payment payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);
            handleSuccess(event);

        } catch (Exception ex) {
            log.error("Error trying to make payment: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(EventDTO event) {
        if (paymentRepository.existsByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionID for the validation");
        }
    }

    public void createPendingPayment(EventDTO event) {
        double totalAmount = calculateAmount(event);
        int totalItems = calculateTotalItems(event);
        Payment payment = Payment
                .builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private void setEventAmountItems(EventDTO event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    private double calculateAmount(EventDTO event) {
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int calculateTotalItems(EventDTO event) {
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(OrderProductsDTO::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    public void realizeRefund(EventDTO event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            changePaymentToRefund(event);
            addHistory(event, "Rollback executed for payment!");
        } catch (Exception e) {
            addHistory(event, "Rollback not excuted for payment: ".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void changePaymentToRefund(EventDTO event) {
        Payment payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

    private void handleFailCurrentNotExecuted(EventDTO event, String errorMessage) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to realize the payment: ".concat(errorMessage));
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

    private Payment findByOrderIdAndTransactionId (EventDTO event) {
        return paymentRepository.findByOrderIdAndTransactionId(event.getOrderId(),
                event.getTransactionId()).orElseThrow(() -> new ValidationException("Payment not found by OrderId: " + event.getOrderId()));
    }


    private void validateAmount(double totalAmount) {
        if(totalAmount < MIN_AMOUNT_VALUE) {
            throw new ValidationException("Minimum amount must be bigger than 0.1: ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    private void  changePaymentToSuccess(Payment payment) {
        payment.setStatus(SUCESS);
        save(payment);
    };

    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "payment realized successfully!");
    }
}
