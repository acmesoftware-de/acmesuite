package de.acmesoftware.acmesuite.search;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.shared.SearchDocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Core Lucene index behaviour (no Spring, no container): index/query, type + audience filters, reindex. */
class SearchIndexTest {

    @TempDir
    Path tmp;

    private SearchIndex index;

    @BeforeEach
    void open() {
        SearchProperties props = new SearchProperties();
        props.setIndexDir(tmp.toString());
        index = new SearchIndex(props);
    }

    @AfterEach
    void close() throws IOException {
        index.close();
    }

    private SearchDocument doc(String type, String id, String module, String title, String body,
            Set<String> audiences) {
        return new SearchDocument(type, id, module, title, null, body, List.of(), audiences, "/x/" + id);
    }

    @Test
    void indexesAndFindsByTitleAndBody() {
        index.index(doc("crm.customer", "1", "crm", "Nordwind Logistik", "CPO Michael Braun", Set.of("all")));
        index.index(doc("crm.customer", "2", "crm", "Meridian Bau", "Einkauf Karin", Set.of("all")));

        assertThat(index.search("Nordwind", null, Set.of("all"), 10)).extracting(SearchIndex.Hit::id)
                .containsExactly("1");
        assertThat(index.search("Braun", null, Set.of("all"), 10)).hasSize(1);
    }

    @Test
    void filtersByType() {
        index.index(doc("crm.customer", "1", "crm", "Alpha", "x", Set.of("all")));
        index.index(doc("supply.supplier", "2", "supply", "Alpha", "x", Set.of("all")));

        assertThat(index.search("Alpha", null, Set.of("all"), 10)).hasSize(2);
        assertThat(index.search("Alpha", Set.of("crm.customer"), Set.of("all"), 10))
                .extracting(SearchIndex.Hit::type).containsExactly("crm.customer");
    }

    @Test
    void filtersByAudience() {
        index.index(doc("hr.employee", "1", "hr", "Secret Person", "x", Set.of("ADMIN")));

        // Everyone-audience user does not see an ADMIN-restricted document...
        assertThat(index.search("Secret", null, Set.of("all"), 10)).isEmpty();
        // ...but an admin (whose audiences include ADMIN) does.
        assertThat(index.search("Secret", null, Set.of("all", "ADMIN"), 10)).hasSize(1);
    }

    @Test
    void removeAndReindexReplacesModule() {
        index.index(doc("crm.customer", "1", "crm", "Gamma", "x", Set.of("all")));
        assertThat(index.search("Gamma", null, Set.of("all"), 10)).hasSize(1);

        index.remove("crm.customer", "1");
        assertThat(index.search("Gamma", null, Set.of("all"), 10)).isEmpty();

        index.replaceModule("crm", List.of(doc("crm.customer", "9", "crm", "Delta", "x", Set.of("all"))));
        assertThat(index.search("Delta", null, Set.of("all"), 10)).extracting(SearchIndex.Hit::id)
                .containsExactly("9");
    }
}
