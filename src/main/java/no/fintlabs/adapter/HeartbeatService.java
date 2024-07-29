package no.fintlabs.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class HeartbeatService {

    private final WebClient webClient;
    private final AdapterRegisterService adapterRegisterService;
    private final AdapterHeartbeat adapterHeartbeat;

    public HeartbeatService(WebClient webClient, AdapterProperties props, AdapterRegisterService adapterRegisterService) {
        this.webClient = webClient;
        this.adapterRegisterService = adapterRegisterService;
        adapterHeartbeat = AdapterHeartbeat.builder()
                .time(System.currentTimeMillis())
                .orgId(props.getOrgId())
                .adapterId(props.getId())
                .username(props.getUsername())
                .build();
    }

    @Scheduled(fixedRateString = "#{@adapterProperties.getHeartbeatIntervalMs()}")
    public void doHeartbeat() {
        if (adapterRegisterService.isRegistered()) {
            log.info("Sending heartbeat FINT...");
            webClient.post()
                    .uri("/provider/heartbeat")
                    .body(Mono.just(adapterHeartbeat), AdapterHeartbeat.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(s -> {
                        log.info("FINT responded: {}", s);
                    });
        } else {
            log.info("Heartbeat service is not started yet!");
            adapterRegisterService.registerAdapter();
        }
    }

}
