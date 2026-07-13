package de.acmesoftware.acmesuite.shared;

import java.util.List;
import java.util.Set;

/**
 * A module's projection of an entity for the central search index. The owning module builds this
 * (it owns the data and its meaning); the {@code search} module owns indexing and querying. Used
 * both as the live feed (carried by {@link SearchDocumentChanged}) and for bulk (re)indexing (via
 * {@link SearchableProvider}) — one shape for both.
 *
 * @param type     stable document type, e.g. {@code "crm.customer"}
 * @param id       entity id (unique within its type)
 * @param module   owning module: {@code crm|supply|build|hr|base}
 * @param title    primary label shown in results
 * @param subtitle secondary line (optional)
 * @param body     free text to full-text index
 * @param keywords extra terms to match on (codes, SKUs, tags …)
 * @param audiences who may see this result — role names and/or {@code "all"}; the query filters to
 *                 the signed-in user's audiences (never returns what they may not see)
 * @param deepLink in-app route to open the entity, e.g. {@code "/crm/customers/42"}
 */
public record SearchDocument(
        String type,
        String id,
        String module,
        String title,
        String subtitle,
        String body,
        List<String> keywords,
        Set<String> audiences,
        String deepLink) {

    /** Audience granting every authenticated user visibility. */
    public static final String AUDIENCE_ALL = "all";

    public SearchDocument {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        audiences = audiences == null || audiences.isEmpty() ? Set.of(AUDIENCE_ALL) : Set.copyOf(audiences);
    }

    /** Composite index key {@code type:id}. */
    public String key() {
        return type + ":" + id;
    }
}
