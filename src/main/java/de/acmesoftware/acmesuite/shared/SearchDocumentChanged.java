package de.acmesoftware.acmesuite.shared;

/**
 * Published by a module after an entity was created or updated, so the central search index can be
 * fed. Consumed asynchronously and durably (transactional outbox) by the {@code search} module —
 * the API write only publishes this event, it never indexes inline.
 */
public record SearchDocumentChanged(SearchDocument document) {
}
