package no.fintlabs.adapter.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.models.*;
import no.fintlabs.adapter.models.sync.*;
import no.fintlabs.adapter.validator.ValidatorService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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
        implements Subscriber<SyncData<T>> {

    @Value("${fint.adapter.page-size:100}")
    private int pageSize;
    private final WebClient webClient;
    private final ValidatorService validatorService;
    protected final AdapterProperties adapterProperties;


    protected ResourceSubscriber(WebClient webClient,
                                 AdapterProperties adapterProperties,
                                 P publisher,
                                 ValidatorService validatorService) {
        this.webClient = webClient;
        this.adapterProperties = adapterProperties;
        this.validatorService = validatorService;

        publisher.subscribe(this);
    }

    public void onSync(SyncData<T> syncData) {
        log.info("Syncing {} items to endpoint {}", syncData.getResources().size(), getCapability().getEntityUri());

        Instant start = Instant.now();
        List<SyncPage> pages = getPages(syncData.getResources(), syncData.getSyncType());
        Flux.fromIterable(pages)
                .flatMap(this::sendPages)
                .doOnComplete(() -> logDuration(syncData.getResources().size(), start))
                .blockLast();
    }

    private void logDuration(int resourceSize, Instant start) {
        Duration timeElapsed = Duration.between(start, Instant.now());
        int amountOfPages = (resourceSize + pageSize - 1) / pageSize;
        log.info("Syncing {} resources in {} pages took {}:{}:{} to complete",
                resourceSize,
                amountOfPages,
                "%02d".formatted(timeElapsed.toHoursPart()),
                "%02d".formatted(timeElapsed.toMinutesPart()),
                "%02d".formatted(timeElapsed.toSecondsPart())
        );
    }

    protected abstract AdapterCapability getCapability();


    protected Mono<?> sendPages(SyncPage page) {
        return webClient
                .method(page.getSyncType().getHttpMethod())
                .uri("/provider" + getCapability().getEntityUri())
                .body(Mono.just(page), SyncPage.class)
                .retrieve()
                .toBodilessEntity()
                .doOnNext(response ->
                        log.info("Page {} returned {}. ({})", page.getMetadata().getPage(), page.getMetadata().getCorrId(), response.getStatusCode())
                );

    }

    public List<SyncPage> getPages(List<T> resources, SyncType syncType) {
        String corrId = UUID.randomUUID().toString();
        int resourceSize = resources.size();
        int amountOfPages = (resourceSize + pageSize - 1) / pageSize;
        List<SyncPage> pages = new ArrayList<>();

        for (int resourceIndex = 0; resourceIndex < resourceSize; resourceIndex += pageSize) {
            SyncPage syncPage = createSyncPage(corrId, resources, syncType, resourceSize, amountOfPages, resourceIndex);
            pages.add(syncPage);
        }

        if (adapterProperties.isDebug()) {
            validatorService.validate(pages, resourceSize);
        }

        return pages;
    }

    private SyncPage createSyncPage(String corrId, List<T> resources, SyncType syncType, int resourceSize, int totalPages, int resourceIndex) {
        List<SyncPageEntry> syncPageEntries = getSyncPageEntries(resources, resourceIndex);
        SyncPageMetadata syncPageMetadata = getSyncPageMetadata(corrId, resourceSize, totalPages, resourceIndex, syncPageEntries);

        return FullSyncPage.builder()
                .metadata(syncPageMetadata)
                .resources(syncPageEntries)
                .syncType(syncType)
                .build();
    }

    private SyncPageMetadata getSyncPageMetadata(String corrId, int resourceAmount, int totalPages, int i, List<SyncPageEntry> entries) {
        return SyncPageMetadata.builder()
                .orgId(adapterProperties.getOrgId())
                .adapterId(adapterProperties.getId())
                .corrId(corrId)
                .totalPages(totalPages)
                .totalSize(resourceAmount)
                .pageSize(entries.size())
                .page((i / pageSize) + 1)
                .uriRef(getCapability().getEntityUri())
                .time(System.currentTimeMillis())
                .build();
    }

    @NotNull
    private List<SyncPageEntry> getSyncPageEntries(List<T> resources, int resourceIndex) {
        int stoppingIndex = Math.min((resourceIndex + pageSize), resources.size());
        return resources
                .subList(resourceIndex, stoppingIndex)
                .stream()
                .map(this::createSyncPageEntry)
                .collect(Collectors.toList());
    }

    protected abstract SyncPageEntry createSyncPageEntry(T resource);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        log.info("Subscribing to resources for endpoint {}", getCapability().getEntityUri());
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(SyncData<T> syncData) {
        onSync(syncData);
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
