package no.fintlabs.adapter.events;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.Flow;

@Slf4j
public abstract class EventSubscriber<T extends FintLinks, P extends EventPublisher<T>>
        implements Flow.Subscriber<ResponseFintEvent<T>> {

    private final WebClient webClient;
    protected final AdapterProperties adapterProperties;
    private final String capabilityKey;

    protected EventSubscriber(WebClient webClient, AdapterProperties adapterProperties, P publisher, String capabilityKey) {
        this.webClient = webClient;
        this.adapterProperties = adapterProperties;
        this.capabilityKey = capabilityKey;

        publisher.subscribe(this);
    }

    public void onSync(ResponseFintEvent<T> responseFintEvent) {

        log.info("Posting response to event {}", responseFintEvent.getCorrId());
        webClient.post()
                .uri("/provider/event")
                .body(Mono.just(responseFintEvent), ResponseFintEvent.class)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> responsePostingEvent(response, responseFintEvent),
                        e -> log.error("Posting response to event failed. Corr-id: " + responseFintEvent.getCorrId(), e)
                );
    }

    protected abstract void responsePostingEvent(ResponseEntity<Void> response, ResponseFintEvent<T> responseFintEvent);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        log.info("Subscribing to resources for endpoint {}", getCapability().getEntityUri());
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ResponseFintEvent<T> resources) {
        onSync(resources);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

    @Override
    public void onComplete() {
        log.info("Subscriber for {} is closed", getCapability().getEntityUri());
    }

    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource(capabilityKey);
    }
}
