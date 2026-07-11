package de.acmesoftware.acmesuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

/**
 * Logs the running version and git commit at startup — the same values exposed on
 * {@code /actuator/info} (from build-info.properties + git.properties). Both are optional so the
 * app still starts if the stamp files are absent (e.g. an IDE run without the build plugins).
 */
@Component
class VersionBanner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VersionBanner.class);

    private final ObjectProvider<BuildProperties> build;
    private final ObjectProvider<GitProperties> git;

    VersionBanner(ObjectProvider<BuildProperties> build, ObjectProvider<GitProperties> git) {
        this.build = build;
        this.git = git;
    }

    @Override
    public void run(ApplicationArguments args) {
        BuildProperties b = build.getIfAvailable();
        GitProperties g = git.getIfAvailable();
        String version = b != null ? b.getVersion() : "unknown";
        String commit = g != null ? g.getShortCommitId() : "unknown";
        String branch = g != null ? g.getBranch() : "unknown";
        boolean dirty = g != null && Boolean.parseBoolean(g.get("dirty"));
        log.info("ACMEsuite {} (git {}{} on {})", version, commit, dirty ? "-dirty" : "", branch);
    }
}
