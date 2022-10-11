package no.fintlabs.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import static java.util.concurrent.Flow.Subscriber;

@Slf4j
public abstract class ResourceSubscriber<T extends FintLinks, P extends ResourcePublisher<T, ResourceRepository<T>>>
        implements Subscriber<List<T>> {

    private final WebClient webClient;
    protected final AdapterProperties adapterProperties;


    protected ResourceSubscriber(WebClient webClient, AdapterProperties adapterProperties, P publisher) {
        this.webClient = webClient;
        this.adapterProperties = adapterProperties;

        publisher.subscribe(this);
    }

    public void onSync(List<T> resources) {
        log.info("Syncing {} items to endpoint {}", resources.size(), getCapability().getEntityUri());

        int pageSize = 100;
        Instant start = Instant.now();
        Flux.fromIterable(getPages(resources, pageSize))
                .flatMap(this::post)
                .doOnComplete(() -> logDuration(resources.size(), pageSize, start))
                .blockLast();
    }

    private static void logDuration(int totalSize, int pageSize, Instant start) {
        Duration timeElapsed = Duration.between(start, Instant.now());
        log.info("Syncing {} elements in {} pages took {}:{}:{} to complete",
                totalSize,
                (totalSize + pageSize - 1) / pageSize,
                String.format("%02d", timeElapsed.toHoursPart()),
                String.format("%02d", timeElapsed.toMinutesPart()),
                String.format("%02d", timeElapsed.toSecondsPart())
        );
    }

    protected abstract AdapterCapability getCapability();


    protected Mono<?> post(SyncPage<T> page) {
        return webClient.post()
                .uri("/provider" + getCapability().getEntityUri())
                .body(Mono.just(page), FullSyncPage.class)
                .retrieve()
                .toBodilessEntity()
                .doOnNext(response -> {
                    log.info("Posting page {} returned {}. ({})", page.getMetadata().getPage(), page.getMetadata().getCorrId(), response.getStatusCode());
                });
    }

    public List<SyncPage<T>> getPages(List<T> resources, int pageSize) {

        List<SyncPage<T>> pages = new ArrayList<>();
        int size = resources.size();
        String corrId = UUID.randomUUID().toString();

        for (int i = 0; i < size; i += pageSize) {
            int end = Math.min((i + pageSize), resources.size());

            List<SyncPageEntry<T>> entries = resources
                    .subList(i, end)
                    .stream()
                    .map(this::createSyncPageEntry)
                    .collect(Collectors.toList());

            pages.add(FullSyncPage.<T>builder()
                    .resources(entries)
                    .metadata(SyncPageMetadata.builder()
                            .orgId(adapterProperties.getOrgId())
                            .adapterId(adapterProperties.getId())
                            .corrId(corrId)
                            .totalPages((size + pageSize - 1) / pageSize)
                            .totalSize(size)
                            .pageSize(entries.size())
                            .page((i / pageSize) + 1)
                            .uriRef(getCapability().getEntityUri())
                            .time(System.currentTimeMillis())
                            .build()
                    )
                    .build());
        }

        return pages;
    }

    protected abstract SyncPageEntry<T> createSyncPageEntry(T resource);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        log.info("Subscribing to resources for endpoint {}", getCapability().getEntityUri());
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(List<T> resources) {
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
}
