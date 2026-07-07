package de.acmesoftware.acmesuite.org.feed;

import de.acmesoftware.acmesuite.org.feed.OrgFeed.OverlayPerson;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.SnapshotFeed;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Org source connector feed. ACME serves these endpoints as the <b>System of Record</b>
 * for the organization; the external platform later pulls them via the {@code OrgSourceConnector} SPI (ADR-041) —
 * alongside the Keycloak connector and the (by then purely hermetic) reference connector.
 */
@RestController
@RequestMapping("/api/org/feed")
class OrgFeedController {

    private final OrgFeedService feed;

    OrgFeedController(OrgFeedService feed) {
        this.feed = feed;
    }

    /** Projected layer (units/hierarchy/persons/memberships/absences). */
    @GetMapping("/snapshot")
    SnapshotFeed snapshot() {
        return feed.snapshot(Instant.now());
    }

    /** Curated layer (title/managerKey/delegateKeys/assistantKeys). */
    @GetMapping("/overlay")
    List<OverlayPerson> overlay() {
        return feed.overlay();
    }
}
