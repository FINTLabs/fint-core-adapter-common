package no.fintlabs.adapter;

import no.fint.model.resource.FintLinks;

import java.util.List;

public interface ResourceRepository<T extends FintLinks> {

    List<T> getResources();

    List<T> getUpdatedResources();
}
