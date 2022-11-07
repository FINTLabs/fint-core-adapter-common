package no.fintlabs.adapter;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdapterServiceFactory {

    private final MultipleAdapterProperties multipleAdapterProperties;

    private final WebClientFactory webClientFactory;

    private List<AdapterRegisterService> registerServices;

    public AdapterServiceFactory(MultipleAdapterProperties multipleAdapterProperties, WebClientFactory webClientFactory) {
        this.multipleAdapterProperties = multipleAdapterProperties;
        this.webClientFactory = webClientFactory;
        registerServices = new ArrayList<>();
    }

    @PostConstruct
    private void startUpServices() {
        for (var adapterProperties : multipleAdapterProperties.getAdapters().entrySet()) {

            AdapterProperties props = adapterProperties.getValue();
            HeartbeatService heartbeatService = new HeartbeatService(webClientFactory.webClient(props), props);
            AdapterRegisterService adapterRegisterService = new AdapterRegisterService(webClientFactory.webClient(props), heartbeatService, props);
            registerServices.add(adapterRegisterService);
        }
    }

}
