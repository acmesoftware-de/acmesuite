package de.acmesoftware.acmesuite.search;

import de.acmesoftware.acmesuite.shared.SearchDocumentChanged;
import de.acmesoftware.acmesuite.shared.SearchDocumentRemoved;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Feeds the central index from module domain events. {@code @ApplicationModuleListener} runs after
 * the publishing transaction commits, asynchronously, and is tracked by the event publication
 * registry (transactional outbox) — so an index update is retried (e.g. on restart) if it fails,
 * and a write is never lost even if the index is momentarily unavailable.
 */
@Component
class SearchIndexListener {

    private final SearchIndex index;

    SearchIndexListener(SearchIndex index) {
        this.index = index;
    }

    @ApplicationModuleListener
    void onChanged(SearchDocumentChanged event) {
        index.index(event.document());
    }

    @ApplicationModuleListener
    void onRemoved(SearchDocumentRemoved event) {
        index.remove(event.type(), event.id());
    }
}
