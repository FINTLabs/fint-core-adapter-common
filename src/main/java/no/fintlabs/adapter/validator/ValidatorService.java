package no.fintlabs.adapter.validator;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.SyncPage;
import no.fintlabs.adapter.models.SyncPageEntry;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ValidatorService {

    public <T extends FintLinks> boolean totalSizeIsValid(List<SyncPage<T>> pages, int totalSize) {
        return pages.size() == totalSize;
    }

    public <T extends FintLinks> boolean totalSizeIsNotValid(List<SyncPage<T>> pages, int totalSize) {
        return pages.size() != totalSize;
    }

    public <T extends FintLinks> boolean noDuplicateIds(List<SyncPage<T>> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .map(SyncPageEntry::getIdentifier)
                .collect(Collectors.toSet())
                .size() == pages.stream()
                .flatMap(p -> p.getResources().stream())
                .map(SyncPageEntry::getIdentifier)
                .count();
    }

    public <T extends FintLinks> boolean duplicateIds(List<SyncPage<T>> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .map(SyncPageEntry::getIdentifier)
                .collect(Collectors.toSet())
                .size() != pages.stream()
                .flatMap(p -> p.getResources().stream())
                .map(SyncPageEntry::getIdentifier)
                .count();
    }

    public <T extends FintLinks> boolean validIds(List<SyncPage<T>> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .allMatch(r -> URLEncoder.encode(r.getIdentifier(), StandardCharsets.UTF_8)
                        .length() == r.getIdentifier().length());
    }

    public <T extends FintLinks> boolean notValidIds(List<SyncPage<T>> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .allMatch(r -> URLEncoder.encode(r.getIdentifier(), StandardCharsets.UTF_8)
                        .length() != r.getIdentifier().length());
    }
}
