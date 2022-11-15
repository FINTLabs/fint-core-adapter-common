package no.fintlabs.adapter;

import lombok.*;
import no.fintlabs.adapter.models.AdapterCapability;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdapterInstanceProperties {

    private int heartbeatInterval;
    private String id;
    private String username;
    private String password;
    private String registrationId;
    private String baseUrl;
    private String orgId;

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
