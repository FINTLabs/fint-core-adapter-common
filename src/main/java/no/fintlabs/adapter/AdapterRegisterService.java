package no.fintlabs.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class AdapterRegisterService {


    private final WebClient webClient;
    private final HeartbeatService heartbeatService;
    private final AdapterProperties props;


    public AdapterRegisterService(WebClient webClient, HeartbeatService heartbeatService, AdapterProperties props) {
        this.webClient = webClient;
        this.heartbeatService = heartbeatService;
        this.props = props;
    }

    @PostConstruct
    public void init() {

        AdapterContract adapterContract = AdapterContract.builder()
                .adapterId(props.getId())
                .orgId(props.getOrgId())
                .time(System.currentTimeMillis())
                .heartbeatIntervalInMinutes(props.getHeartbeatInterval())
                .username(props.getUsername())
                .capabilities(props.adapterCapabilityToSet())
                .build();

        webClient.post()
                .uri("/provider/register")
                .body(Mono.just(adapterContract), AdapterContract.class)
                .retrieve()
                .toBodilessEntity()
                .subscribe(response -> {
                    log.info("Register return with code {}.", response.getStatusCode().value());
                    heartbeatService.start();
                });

        log.info("Keep on rocking in a free world ✌️🌻️🇺🇦!");
    }
}
