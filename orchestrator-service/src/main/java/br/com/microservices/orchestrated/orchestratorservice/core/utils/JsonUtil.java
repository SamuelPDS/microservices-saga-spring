package br.com.microservices.orchestrated.orchestratorservice.core.utils;

import br.com.microservices.orchestrated.orchestratorservice.core.model.dto.EventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JsonUtil {
    private final ObjectMapper objectMapper;

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            return "";
        }
    }

    public EventDTO toEvent(String json) {
        try {
            return objectMapper.readValue(json, EventDTO.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
