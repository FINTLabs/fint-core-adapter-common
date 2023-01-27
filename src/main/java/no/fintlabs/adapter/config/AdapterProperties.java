package no.fintlabs.adapter.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.adapter.models.AdapterCapability;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "fint.adapter")
public class AdapterProperties {

    private int heartbeatInterval;
    private String id;
    private String username;
    private String password;
    private String registrationId;
    private String baseUrl;
    private String orgId;
    private boolean debug;

    private Map<String, AdapterCapability> capabilities;

    public Set<AdapterCapability> adapterCapabilityToSet() {
        return new HashSet<>(capabilities.values());
    }
    public long getHeartbeatIntervalMs() {
        return Duration.parse("PT" + heartbeatInterval + "M").toMillis();
    }

    public long getFullSyncIntervalMs(String entity) {
        return Duration.parse("PT" + capabilities.get(entity).getFullSyncIntervalInDays() + "H").toMillis();
    }

    public AdapterCapability getCapabilityByResource(String resource) {
        return capabilities.get(resource);
    }
}
