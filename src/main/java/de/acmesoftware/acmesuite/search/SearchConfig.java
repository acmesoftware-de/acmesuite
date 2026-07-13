package de.acmesoftware.acmesuite.search;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous event handling so {@code @ApplicationModuleListener} (the search index feed)
 * runs off the request thread, after commit, tracked by the event publication registry.
 */
@Configuration
@EnableAsync
class SearchConfig {
}
