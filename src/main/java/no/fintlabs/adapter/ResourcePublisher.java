package no.fintlabs.adapter;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.AdapterCapability;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

public abstract class ResourcePublisher<T extends FintLinks, R extends ResourceRepository<T>> extends SubmissionPublisher<List<T>> {

    protected final R repository;
    protected final AdapterInstanceProperties adapterInstanceProperties;


    public ResourcePublisher(R repository, AdapterInstanceProperties adapterInstanceProperties) {
        this.repository = repository;
        this.adapterInstanceProperties = adapterInstanceProperties;
    }

    public abstract void doFullSync();

    public abstract void doDeltaSync();

    protected abstract AdapterCapability getCapability();

}

