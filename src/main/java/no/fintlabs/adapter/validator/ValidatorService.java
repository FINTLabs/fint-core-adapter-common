package no.fintlabs.adapter.validator;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.sync.SyncPage;
import no.fintlabs.adapter.models.sync.SyncPageEntry;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class ValidatorService {

    public void validate(List<SyncPage> pages, int resourceSize) {
        if (!validPageSize(pages))
            log.warn("Page size does not match resources size!");
        if (!validTotalSize(pages, resourceSize))
            log.warn("Total size doesnt match amount of resources!");
        if (hasDuplicateIds(pages))
            log.warn("Duplicate ids found!");
        if (!validIds(pages))
            log.warn("One or more Id's is not valid");
    }

    public boolean validPageSize(List<SyncPage> pages) {
        return pages.stream().allMatch(page -> page.getMetadata().getPageSize() == page.getResources().size());
    }

    public boolean validTotalSize(List<SyncPage> pages, int totalSize) {
        return pages.stream().mapToInt(page -> page.getResources().size()).sum() == totalSize;
    }

    public boolean hasDuplicateIds(List<SyncPage> pages) {
        HashSet<String> uniqueIds = new HashSet<>();
        for (SyncPage page : pages) {
            for (SyncPageEntry entry : page.getResources()) {
                if (!uniqueIds.add(entry.getIdentifier())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean validIds(List<SyncPage> pages) {
        return pages.stream()
                .flatMap(p -> p.getResources().stream())
                .allMatch(r -> r.getIdentifier().matches("^urn:[a-z0-9][a-z0-9-]{1,31}:([a-z0-9()+,-.:=@;$_!*']|%[0-9a-f]{2})*$"));
    }
}
