package de.acmesoftware.acmesuite.base.auth;

/**
 * One field in an {@link AuthProvider}'s configuration schema. The Admin UI renders the provider
 * config form generically from this schema, so a new provider needs no bespoke UI. Fields of type
 * {@link Type#SECRET} are stored envelope-encrypted and never returned in clear.
 */
public record ConfigField(String key, String label, Type type, boolean required) {

    public enum Type {
        TEXT,
        SECRET,
        URL,
        BOOL
    }

    public static ConfigField text(String key, String label, boolean required) {
        return new ConfigField(key, label, Type.TEXT, required);
    }

    public static ConfigField url(String key, String label, boolean required) {
        return new ConfigField(key, label, Type.URL, required);
    }

    public static ConfigField secret(String key, String label, boolean required) {
        return new ConfigField(key, label, Type.SECRET, required);
    }

    public static ConfigField bool(String key, String label) {
        return new ConfigField(key, label, Type.BOOL, false);
    }
}
