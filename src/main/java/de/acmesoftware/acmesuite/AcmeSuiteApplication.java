package de.acmesoftware.acmesuite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/**
 * ACMEsuite — the application of ACME analog Inc. (CRM/HR/Supply/Build) on the foundation
 * <em>ACMEbase</em> (DB + Auth + Roles).
 *
 * <p>Standalone system: consumers use the suite exclusively via its REST API (ADR-0006). Built as
 * a modular monolith (Spring Modulith); modules are introduced stage by stage (base, shared, org,
 * crm, supply, build, …).
 */
@Modulithic(systemName = "ACMEsuite")
@ConfigurationPropertiesScan
@SpringBootApplication
public class AcmeSuiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeSuiteApplication.class, args);
    }
}
