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
    private final AdapterProperties props;

    private boolean started;

    public HeartbeatService(WebClient webClient, AdapterProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public void start() {
        log.info("Started heartbeat service.");
        started = true;
    }

    public void stop() {
        log.info("Stopped heartbeat service.");
        started = false;
    }


    @Scheduled(fixedRateString = "#{@adapterProperties.getHeartbeatIntervalMs()}")
    public void doHeartbeat() {

        if (started) {
            log.info("Sending heartbeat FINT...");
            AdapterHeartbeat adapterHeartbeat = AdapterHeartbeat.builder()
                    .time(System.currentTimeMillis())
                    .orgId(props.getOrgId())
                    .adapterId(props.getId())
                    .username(props.getUsername())
                    .build();

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
        }
    }


}
