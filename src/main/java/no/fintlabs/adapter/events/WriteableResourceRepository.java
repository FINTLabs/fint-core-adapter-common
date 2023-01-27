package no.fintlabs.adapter.events;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.datasync.ResourceRepository;
import no.fintlabs.adapter.models.RequestFintEvent;

public interface WriteableResourceRepository<T extends FintLinks> extends ResourceRepository<T> {

    T saveResources(T resource, RequestFintEvent requestFintEvent);
}
