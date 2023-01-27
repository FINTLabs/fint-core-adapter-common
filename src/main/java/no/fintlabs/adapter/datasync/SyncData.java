package no.fintlabs.adapter.datasync;

import lombok.Data;
import no.fint.model.resource.FintLinks;

import java.util.List;

@Data
public class SyncData<T extends FintLinks> {

    private final List<T> resources;
    private final SyncDataMethod method;

}
