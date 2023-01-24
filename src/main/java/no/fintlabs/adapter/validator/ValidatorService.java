package no.fintlabs.adapter.validator;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.SyncPage;
import no.fintlabs.adapter.models.SyncPageEntry;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ValidatorService<T extends FintLinks> {

    public void validate(List<SyncPage<T>> pages, int totalSize) {
        if (!validPageSize(pages))
            log.warn("Page size does not match resources size!");
        if (!validTotalSize(pages, totalSize))
            log.warn("Total size doesnt match amount of elements!");
        if (hasDuplicateIds(pages))
            log.warn("Duplicate ids found!");
        if (!validIds(pages))
            log.warn("One or more Id's is not valid");
    }

    public boolean validPageSize(List<SyncPage<T>> pages) {
        return pages.stream().allMatch(page -> page.getMetadata().getPageSize() == page.getResources().size());
    }

    public boolean validTotalSize(List<SyncPage<T>> pages, int totalSize) {
        return pages.stream().mapToInt(page -> page.getResources().size()).sum() == totalSize;
    }

    public boolean hasDuplicateIds(List<SyncPage<T>> pages) {
        HashSet<String> uniqueIds = new HashSet<>();
        for (SyncPage<T> page : pages) {
            for (SyncPageEntry entry : page.getResources()) {
                if (!uniqueIds.add(entry.getIdentifier())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean validIds(List<SyncPage<T>> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .allMatch(r -> r.getIdentifier().matches("^urn:[a-z0-9][a-z0-9-]{1,31}:([a-z0-9()+,-.:=@;$_!*']|%[0-9a-f]{2})*$"));
    }
}
