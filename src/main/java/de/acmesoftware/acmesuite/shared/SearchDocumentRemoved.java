package de.acmesoftware.acmesuite.shared;

/** Published by a module after an entity was deleted, so it is dropped from the search index. */
public record SearchDocumentRemoved(String type, String id) {
}
