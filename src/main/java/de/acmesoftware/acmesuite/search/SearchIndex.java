package de.acmesoftware.acmesuite.search;

import de.acmesoftware.acmesuite.shared.SearchDocument;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

/**
 * The single owner of the central Lucene index: upsert/remove/query and bulk replace (for
 * reindex). Documents are keyed by {@code type:id}; queries match title/subtitle/keywords/body
 * (boosted), filtered by the caller's audiences and (optionally) a set of types. One IndexWriter
 * per JVM; a SearcherManager gives near-real-time reads.
 */
@Component
class SearchIndex implements AutoCloseable {

    private static final Map<String, Float> BOOSTS =
            Map.of("title", 4f, "subtitle", 2f, "keywords", 2f, "body", 1f);
    private static final String[] TEXT_FIELDS = {"title", "subtitle", "keywords", "body"};

    private final Analyzer analyzer = new StandardAnalyzer();
    private final Directory directory;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;

    SearchIndex(SearchProperties props) {
        try {
            String dir = props.getIndexDir();
            // ":memory:" (or blank) → an in-memory index (tests, ephemeral rebuild-from-source).
            this.directory = dir == null || dir.isBlank() || ":memory:".equals(dir)
                    ? new ByteBuffersDirectory()
                    : FSDirectory.open(Path.of(dir));
            this.writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            this.writer.commit(); // materialize an empty index on first start
            this.searcherManager = new SearcherManager(writer, new SearcherFactory());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot open search index at " + props.getIndexDir(), e);
        }
    }

    void index(SearchDocument doc) {
        try {
            writer.updateDocument(new Term("key", doc.key()), toLucene(doc));
            refresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void remove(String type, String id) {
        try {
            writer.deleteDocuments(new Term("key", type + ":" + id));
            refresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Replaces every document of a module (used for reindex). */
    void replaceModule(String module, Iterable<SearchDocument> docs) {
        try {
            writer.deleteDocuments(new Term("module", module));
            for (SearchDocument doc : docs) {
                writer.updateDocument(new Term("key", doc.key()), toLucene(doc));
            }
            refresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<Hit> search(String queryText, Set<String> types, Set<String> audiences, int limit) {
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }
        try {
            BooleanQuery.Builder query = new BooleanQuery.Builder()
                    .add(textQuery(queryText), Occur.MUST)
                    .add(terms("audience", audiences), Occur.FILTER);
            if (types != null && !types.isEmpty()) {
                query.add(terms("type", types), Occur.FILTER);
            }
            IndexSearcher searcher = searcherManager.acquire();
            try {
                TopDocs top = searcher.search(query.build(), Math.max(1, limit));
                StoredFields stored = searcher.storedFields();
                List<Hit> hits = new ArrayList<>(top.scoreDocs.length);
                for (ScoreDoc sd : top.scoreDocs) {
                    Document d = stored.document(sd.doc);
                    hits.add(new Hit(d.get("type"), d.get("id"), d.get("module"),
                            d.get("title"), d.get("subtitle"), d.get("deepLink"), sd.score));
                }
                return hits;
            } finally {
                searcherManager.release(searcher);
            }
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("search failed", e);
        }
    }

    private Query textQuery(String text) throws ParseException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(TEXT_FIELDS, analyzer, BOOSTS);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        return parser.parse(QueryParserBase.escape(text));
    }

    private static Query terms(String field, Set<String> values) {
        return new TermInSetQuery(field, values.stream().map(BytesRef::new).toList());
    }

    private static Document toLucene(SearchDocument d) {
        Document doc = new Document();
        doc.add(new StringField("key", d.key(), Field.Store.NO));
        doc.add(new StringField("type", d.type(), Field.Store.YES));
        doc.add(new StringField("id", d.id(), Field.Store.YES));
        doc.add(new StringField("module", d.module(), Field.Store.YES));
        doc.add(new TextField("title", nz(d.title()), Field.Store.YES));
        doc.add(new TextField("subtitle", nz(d.subtitle()), Field.Store.YES));
        doc.add(new StoredField("deepLink", nz(d.deepLink())));
        doc.add(new TextField("body", nz(d.body()), Field.Store.NO));
        doc.add(new TextField("keywords", String.join(" ", d.keywords()), Field.Store.NO));
        for (String audience : d.audiences()) {
            doc.add(new StringField("audience", audience, Field.Store.NO));
        }
        return doc;
    }

    private void refresh() throws IOException {
        writer.commit();
        searcherManager.maybeRefreshBlocking();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void close() throws IOException {
        searcherManager.close();
        writer.close();
        directory.close();
    }

    /** One search result (stored fields only). */
    record Hit(String type, String id, String module, String title, String subtitle, String deepLink,
            float score) {
    }
}
