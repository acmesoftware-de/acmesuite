package de.acmesoftware.acmesuite.base;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the ACMEbase auth ({@code acme.base.auth.*}).
 *
 * <p>Default {@code enabled=false}: the suite APIs are open — suitable for local development, and
 * keeps the existing test suite green. Set to {@code true} to enable authentication; then the
 * role rules ({@link AccessRole}) apply via HTTP Basic.
 */
@ConfigurationProperties("acme.base.auth")
public class AuthProperties {

    /** Role enforcement on the suite APIs on/off. */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
