package no.fintlabs.adapter.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ResourceConverter {
    private ObjectMapper objectMapper;

    public ResourceConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T from(String input, Class clazz) throws JsonProcessingException {
        return (T) objectMapper.readValue(input, clazz);
    }
}
