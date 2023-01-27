package no.fintlabs.adapter.datasync;

import lombok.Data;
import no.fint.model.resource.FintLinks;

import java.util.List;

@Data
public class SyncData<T extends FintLinks> {

    private final List<T> resources;
    private final SyncDataMethod method;

    public static <T extends FintLinks> SyncData<T> ofPostData(List<T> resources) {
        return new SyncData<>(resources, SyncDataMethod.POST);
    }

    public static <T extends FintLinks> SyncData<T> ofPatchData(List<T> resources) {
        return new SyncData<>(resources, SyncDataMethod.PATCH);
    }

}
