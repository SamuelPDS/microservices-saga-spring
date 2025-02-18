package br.com.microservices.orchestrated.orchestratorservice.core.kafka.producer;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor // cria o construtor com os campos necessarios
@Component
public class SagaOrchestratorProducer {
    private final KafkaTemplate<String,String> kafkaTemplate;

    public void sendEvent(String payload, String orchestratorTopic) {
        try {
            log.info("sending event to Topic {} with data {}", orchestratorTopic, payload);
            kafkaTemplate.send(orchestratorTopic, payload);
        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", orchestratorTopic, payload);
        }
    }
}
