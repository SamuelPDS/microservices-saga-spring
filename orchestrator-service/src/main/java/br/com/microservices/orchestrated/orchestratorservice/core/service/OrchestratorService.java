package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.kafka.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.model.dto.EventDTO;
import br.com.microservices.orchestrated.orchestratorservice.core.model.dto.HistoryDTO;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionController;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class OrchestratorService {
    private final JsonUtil jsonUtil;
    private final SagaOrchestratorProducer producer;
    private final SagaExecutionController executionController;

    public void startSaga(EventDTO event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.SUCCESS);
        ETopics topic = executionController.getNextTopic(event);
        log.info("SAGA STARTED, SENDING TO TOPIC {}", topic);
        addHistory(event, "Saga started!");
        sendToTopic(event, topic);
    }

    public void finishSagaSuccess(EventDTO event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.SUCCESS);
        log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT {}", event.getId());
        addHistory(event, "Saga finished successfully!");
        notifyFinishedSaga(event);
    }

    public void finishSagaFail(EventDTO event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.FAIL);
        log.info("SAGA FAILED FOR EVENT {}", event.getId());
        addHistory(event, "Saga failed!");
        notifyFinishedSaga(event);
    }

    public void continueSaga(EventDTO event) {
        ETopics topic = executionController.getNextTopic(event);
        log.info("CONTINUING SAGA, SENDING TO TOPIC {}", topic);
        sendToTopic(event, topic);
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

    private void notifyFinishedSaga(EventDTO event) {
        sendToTopic(event, ETopics.NOTIFY_ENDING);
    }

    private void sendToTopic(EventDTO event, ETopics topic) {
        producer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
    }
}
