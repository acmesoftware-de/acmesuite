package de.acmesoftware.acmesuite.search;

import java.util.List;

/** DTOs of the search HTTP API. */
public final class SearchViews {

    private SearchViews() {
    }

    public record Hit(String type, String id, String module, String title, String subtitle,
            String deepLink, float score) {
    }

    public record Results(String query, List<Hit> hits) {
    }

    public record ReindexResult(String module, int documents) {
    }

    static Hit hit(SearchIndex.Hit h) {
        return new Hit(h.type(), h.id(), h.module(), h.title(), h.subtitle(), h.deepLink(), h.score());
    }
}
