package de.acmesoftware.acmesuite.search;

import de.acmesoftware.acmesuite.shared.SearchDocument;
import de.acmesoftware.acmesuite.shared.SearchableProvider;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Query + reindex over the central index. Queries are filtered to the caller's audiences (never
 * returning what the user may not see). Reindex pulls the source of truth from each module's
 * {@link SearchableProvider} — the index is a derived projection.
 */
@Service
public class SearchService {

    private final SearchIndex index;
    private final List<SearchableProvider> providers;
    private final SearchProperties props;

    public SearchService(SearchIndex index, List<SearchableProvider> providers, SearchProperties props) {
        this.index = index;
        this.providers = providers;
        this.props = props;
    }

    public List<SearchViews.Hit> query(String queryText, Set<String> types, Set<String> audiences,
            Integer limit) {
        int effective = limit == null ? props.getDefaultLimit() : Math.min(limit, props.getMaxLimit());
        return index.search(queryText, types, audiences, effective).stream()
                .map(SearchViews::hit)
                .toList();
    }

    /** Rebuilds the index from the modules. {@code module == null} reindexes everything. */
    public int reindex(String module) {
        int count = 0;
        for (SearchableProvider provider : providers) {
            if (module != null && !module.equalsIgnoreCase(provider.module())) {
                continue;
            }
            List<SearchDocument> docs = provider.all().toList();
            index.replaceModule(provider.module(), docs);
            count += docs.size();
        }
        return count;
    }
}
