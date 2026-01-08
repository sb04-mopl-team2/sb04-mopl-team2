package com.codeit.mopl.outbox.util;

import com.codeit.mopl.exception.outbox.EventDeserializationFailedException;
import com.codeit.mopl.exception.outbox.EventSerializationFailedException;
import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object event) {
        String eventClassName = event.getClass().getSimpleName();
        try {
            log.debug("[OutBox] payload 직렬화 수행: eventClass = {}", eventClassName);
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("[OutBox] payload 직렬화 실패: eventClass = {}, errorMessage = {}", eventClassName, e.getMessage(), e);
            throw EventSerializationFailedException.withDetails(event);
        }
    }

    public <T> T deserialize(OutBoxEvent event, Class<T> eventClass) {
        String eventClassName = event.getClass().getSimpleName();
        try {
            log.debug("[OutBox] payload 역직렬화 수행: eventClass = {}", eventClassName);
            return objectMapper.readValue(event.getPayload(), eventClass);
        } catch (JsonProcessingException e) {
            log.error("[OutBox] payload 역직렬화 실패: eventClass = {}, errorMessage = {}", eventClassName, e.getMessage(), e);
            throw EventDeserializationFailedException.withDetails(event);
        }
    }
}
