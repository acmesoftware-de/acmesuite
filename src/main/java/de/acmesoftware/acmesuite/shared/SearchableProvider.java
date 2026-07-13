package de.acmesoftware.acmesuite.shared;

import java.util.stream.Stream;

/**
 * SPI a module implements to expose its searchable entities as the source of truth for (re)indexing
 * — the index is a derived projection, rebuilt from the modules on demand (first start, analyzer
 * change, corruption). The {@code search} module collects all providers. The live feed uses
 * {@link SearchDocumentChanged}/{@link SearchDocumentRemoved}; both paths share {@link SearchDocument}.
 */
public interface SearchableProvider {

    /** Owning module ({@code crm|supply|build|hr|base}) — lets an admin reindex one module. */
    String module();

    /** All current documents of this module (streamed; may be large). */
    Stream<SearchDocument> all();
}
