package no.fintlabs.adapter;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("fint")
public class AdapterCollectionProperties {

    public AdapterCollectionProperties() {
        adapter = new HashMap<String, AdapterProperties>();
    }

    @Getter
    private Map<String, AdapterProperties> adapter;

}
