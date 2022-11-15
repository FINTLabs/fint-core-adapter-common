package no.fintlabs.adapter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

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

            AdapterInstanceProperties props = adapterProperties.getValue();
            HeartbeatService heartbeatService = new HeartbeatService(webClientFactory.webClient(props), props);
            AdapterRegisterService adapterRegisterService = new AdapterRegisterService(webClientFactory.webClient(props), heartbeatService, props);
            registerAdapterService("register-service-" + adapterProperties.getKey(), adapterRegisterService);

            registerAdapterProperties(adapterProperties.getKey(), props);
        }
    }

    private void registerAdapterProperties(String beanQualifier, AdapterInstanceProperties props) {
        genericApplicationContext.registerBean(
                beanQualifier,
                AdapterInstanceProperties.class,
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

    private void validate(AdapterInstanceProperties props, String entryKey){
        if (StringUtils.isEmpty(props.getId())) throw new IllegalArgumentException("AdapterProperties for " + entryKey + " is missing id.");
    }

}
