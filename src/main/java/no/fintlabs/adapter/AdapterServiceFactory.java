package no.fintlabs.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdapterServiceFactory {

    private final AdapterCollectionProperties multipleAdapterCollectionProperties;

    private final WebClientFactory webClientFactory;

    private final GenericApplicationContext genericApplicationContext;

    public AdapterServiceFactory(AdapterCollectionProperties multipleAdapterCollectionProperties, WebClientFactory webClientFactory, GenericApplicationContext genericApplicationContext) {
        this.multipleAdapterCollectionProperties = multipleAdapterCollectionProperties;
        this.webClientFactory = webClientFactory;
        this.genericApplicationContext = genericApplicationContext;

        if (multipleAdapterCollectionProperties.getAdapter().size() == 0)
            throw new IllegalArgumentException("No entry for fint.adapter found");

        for (var adapterProperties : multipleAdapterCollectionProperties.getAdapter().entrySet()) {

            validate(adapterProperties.getValue(), adapterProperties.getKey());

            AdapterProperties props = adapterProperties.getValue();
            HeartbeatService heartbeatService = new HeartbeatService(webClientFactory.webClient(props), props);
            AdapterRegisterService adapterRegisterService = new AdapterRegisterService(webClientFactory.webClient(props), heartbeatService, props);
            registerAdapterService("register-service-" + adapterProperties.getKey(), adapterRegisterService);
            log.info("Registert adapter service: " + adapterProperties.getKey());

            registerAdapterProperties(adapterProperties.getKey(), props);
            log.info("Register adapter properties: " + adapterProperties.getKey());
        }
    }

    private void registerAdapterProperties(String beanQualifier, AdapterProperties props) {
        genericApplicationContext.registerBean(
                beanQualifier,
                AdapterProperties.class,
                () -> props
        );
    }

    private void registerAdapterService(String beanQualifier, AdapterRegisterService adapterRegisterService) {
        genericApplicationContext.registerBean(
                beanQualifier,
                AdapterRegisterService.class,
                () -> adapterRegisterService
        );
    }

    private void validate(AdapterProperties props, String entryKey){
        if (StringUtils.isEmpty(props.getId())) throw new IllegalArgumentException("AdapterProperties for " + entryKey + " is missing id.");
    }

}
