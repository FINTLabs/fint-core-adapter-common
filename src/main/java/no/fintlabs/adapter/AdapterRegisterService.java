package no.fintlabs.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.models.AdapterContract;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class AdapterRegisterService {

    private final WebClient webClient;
    private final HeartbeatService heartbeatService;
    private final AdapterContract adapterContract;

    public AdapterRegisterService(WebClient webClient, HeartbeatService heartbeatService, AdapterProperties props) {
        this.webClient = webClient;
        this.heartbeatService = heartbeatService;
        adapterContract = AdapterContract.builder()
                .adapterId(props.getId())
                .orgId(props.getOrgId())
                .time(System.currentTimeMillis())
                .heartbeatIntervalInMinutes(props.getHeartbeatInterval())
                .username(props.getUsername())
                .capabilities(props.adapterCapabilityToSet())
                .build();
        registerAdapter();
    }

    private void registerAdapter() {
        webClient.post()
                .uri("/provider/register")
                .body(Mono.just(adapterContract), AdapterContract.class)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5))
                        .filter(throwable -> {
                            log.error("Registration failed, retrying...", throwable);
                            return true;
                        }))
                .subscribe(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Register return with code {}.", response.getStatusCode().value());
                        heartbeatService.start();
                    } else {
                        log.error("Failed to register with code {}.", response.getStatusCode().value());
                    }
                }, throwable -> log.error("Failed to register after retries.", throwable));

        log.info("Keep on rocking in a free world ✌️🌻️🇺🇦!");
    }

    @Scheduled(fixedDelay = 30000)
    public void checkIfRegistered() {
        if (!heartbeatService.isStarted()) {
            log.info("Heartbeat service is not running or not registered, attempting to re-register...");
            registerAdapter();
        }
    }

}
