package no.fintlabs.adapter.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.AdapterProperties;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

@Slf4j
public abstract class EventPublisher<T extends FintLinks> extends SubmissionPublisher<ResponseFintEvent<T>> {

    private final ResourceConverter resourceConverter;
    private final WebClient webClient;
    protected final AdapterProperties adapterProperties;
    private final String capabilityKey;
    private final Class<T> classOfT;
    protected final WriteableResourceRepository<T> repository;

    protected EventPublisher(String capabilityKey, Class<T> classOfT, ResourceConverter resourceConverter, WebClient webClient, AdapterProperties adapterProperties, WriteableResourceRepository<T> repository) {
        this.classOfT = classOfT;
        this.capabilityKey = capabilityKey;
        this.resourceConverter = resourceConverter;
        this.webClient = webClient;
        this.adapterProperties = adapterProperties;
        this.repository = repository;
    }

    public abstract void doCheckForNewEvents();

    protected void checkForNewEvents() {
        log.info("Check events for resource {}", getCapability().getEntityUri());

        AdapterCapability adapterCapability = adapterProperties.getCapabilities().get(capabilityKey);
        String uri = String.format("/provider/event/%s/%s/%s/", adapterCapability.getDomainName(), adapterCapability.getPackageName(), adapterCapability.getResourceName());

        webClient.get()
                .uri(uri)
                .retrieve()
                .toEntityList(RequestFintEvent.class)
                .doOnError(exception -> log.error("Error checking for new events", exception))
                .subscribe(this::handleEvents);
    }

    protected ResponseFintEvent<T> createResponse(RequestFintEvent requestFintEvent) {
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
        log.info("Event received with {} elements", body.size());

        body.forEach(requestEvent -> {

            try {
                T resource = resourceConverter.from(requestEvent.getValue(), classOfT);
                handleEvent(requestEvent, resource);
            } catch (JsonProcessingException e) {
                // TODO: 21/12/2022 Handle error?
                throw new RuntimeException(e);
            }

        });
    }

    protected abstract void handleEvent(RequestFintEvent requestFintEvent, T resource);

    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource(capabilityKey);
    }
}
