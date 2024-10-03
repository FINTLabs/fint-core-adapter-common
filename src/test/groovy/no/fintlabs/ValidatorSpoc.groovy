package no.fintlabs

import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.adapter.validator.ValidatorService
import org.junit.jupiter.api.Disabled
import spock.lang.Specification

@Disabled
class ValidatorServiceSpec extends Specification {

    def 'validIds should return false if at least one id is not valid'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'urn:valid:id1'),
                        new SyncPageEntry(identifier: 'urn:valid:id2'),
                        new SyncPageEntry(identifier: 'invalid')
                ],
                SyncType.FULL
        )
        def pages = [page1]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validIds(pages)

        then:
        !result
    }

    def 'validIds should return true if all ids are valid'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 3),
                [
                        new SyncPageEntry(identifier: 'urn:valid:id1'),
                        new SyncPageEntry(identifier: 'urn:valid:id2'),
                        new SyncPageEntry(identifier: 'urn:valid:id3')
                ],
                SyncType.FULL
        )
        def pages = [page1]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validIds(pages)

        then:
        result
    }

    def 'hasDuplicateIds should return false if no duplicate IDs are found'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id1')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id2')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.hasDuplicateIds(pages)

        then:
        !result
    }

    def 'hasDuplicateIds should return true if duplicate IDs are found'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 2),
                [
                        new SyncPageEntry(identifier: 'id1'),
                        new SyncPageEntry(identifier: 'id2')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id2')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.hasDuplicateIds(pages)

        then:
        result
    }

    def 'validPageSize should return true if page size matches resources size'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id1')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id2')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validPageSize(pages)

        then:
        result
    }

    def 'validPageSize should return false if page size does not match resources size'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 2),
                [
                        new SyncPageEntry(identifier: 'id1')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id2')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validPageSize(pages)

        then:
        !result
    }

    def 'validTotalSize should return true if total size matches amount of elements'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id1')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 2),
                [
                        new SyncPageEntry(identifier: 'id2'),
                        new SyncPageEntry(identifier: 'id3')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def totalSize = 3
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validTotalSize(pages, totalSize)

        then:
        result
    }

    def 'validTotalSize should return false if total size does not match amount of elements'() {
        given:
        def page1 = new SyncPage(
                new SyncPageMetadata(pageSize: 1),
                [
                        new SyncPageEntry(identifier: 'id1')
                ],
                SyncType.FULL
        )
        def page2 = new SyncPage(
                new SyncPageMetadata(pageSize: 2),
                [
                        new SyncPageEntry(identifier: 'id2'),
                        new SyncPageEntry(identifier: 'id3')
                ],
                SyncType.FULL
        )
        def pages = [page1, page2]
        def totalSize = 2
        def validatorService = new ValidatorService()

        when:
        def result = validatorService.validTotalSize(pages, totalSize)

        then:
        !result
    }
}