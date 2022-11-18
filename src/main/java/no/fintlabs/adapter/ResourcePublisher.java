package no.fintlabs.adapter;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.AdapterCapability;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

public abstract class ResourcePublisher<T extends FintLinks, R extends ResourceRepository<T>> extends SubmissionPublisher<List<T>> {

    protected final R repository;
    protected final AdapterProperties adapterProperties;


    public ResourcePublisher(R repository, AdapterProperties adapterProperties) {
        this.repository = repository;
        this.adapterProperties = adapterProperties;
    }

    public abstract void doFullSync();

    public abstract void doDeltaSync();

    protected abstract AdapterCapability getCapability();

}

