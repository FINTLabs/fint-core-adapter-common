package no.fintlabs.adapter.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

@Slf4j
public abstract class EventPublisher<T extends FintLinks> extends SubmissionPublisher<ResponseFintEvent> {

    protected final AdapterProperties adapterProperties;
    protected final WriteableResourceRepository<T> repository;
    private final WebClient webClient;
    private final String capabilityKey;
    private final Class<T> classOfT;
    private final ObjectMapper objectMapper;

    protected EventPublisher(String capabilityKey, Class<T> classOfT, WebClient webClient, AdapterProperties adapterProperties, WriteableResourceRepository<T> repository, ObjectMapper objectMapper) {
        this.classOfT = classOfT;
        this.capabilityKey = capabilityKey;
        this.webClient = webClient;
        this.adapterProperties = adapterProperties;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public abstract void doCheckForNewEvents();

    protected void checkForNewEvents() {
        log.debug("Check events for resource {}", getCapability().getEntityUri());

        AdapterCapability adapterCapability = adapterProperties.getCapabilities().get(capabilityKey);
        String uri = "/provider/event/%s/%s/%s/".formatted(adapterCapability.getDomainName(), adapterCapability.getPackageName(), adapterCapability.getResourceName());

        webClient.get()
                .uri(uri)
                .retrieve()
                .toEntityList(RequestFintEvent.class)
                .doOnError(exception -> log.error("Error checking for new events", exception))
                .subscribe(this::handleEvents);
    }

    protected ResponseFintEvent createResponse(RequestFintEvent requestFintEvent) {
        return ResponseFintEvent.<T>builder()
                .corrId(requestFintEvent.getCorrId())
                .orgId(adapterProperties.getOrgId())
                .adapterId(adapterProperties.getId())
                .handledAt(System.currentTimeMillis())
                .build();
    }

    private void handleEvents(ResponseEntity<List<RequestFintEvent>> response) {
        log.debug("Event return with code {}.", response.getStatusCode().value());
        // TODO: 21/12/2022 Handle errors?

        List<RequestFintEvent> body = response.getBody();
        assert body != null;
        if (!body.isEmpty()) log.info("Event received with {} elements", body.size());
        else log.debug("Event received with {} elements", 0);

        body.forEach(requestEvent -> {
            T resource = null;
            try {
                resource = objectMapper.readValue(requestEvent.getValue(), classOfT);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            handleEvent(requestEvent, resource);
        });
    }

    protected abstract void handleEvent(RequestFintEvent requestFintEvent, T resource);

    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource(capabilityKey);
    }
}
