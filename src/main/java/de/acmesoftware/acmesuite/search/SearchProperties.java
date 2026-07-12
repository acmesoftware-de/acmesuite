package de.acmesoftware.acmesuite.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration of the central search index ({@code acme.search.*}). */
@ConfigurationProperties("acme.search")
public class SearchProperties {

    /** Filesystem directory holding the Lucene index. */
    private String indexDir = "data/search-index";
    /** Default number of hits returned when the caller does not specify a limit. */
    private int defaultLimit = 20;
    /** Upper bound on hits per query. */
    private int maxLimit = 100;

    public String getIndexDir() {
        return indexDir;
    }

    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }
}
