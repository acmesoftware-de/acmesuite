package de.acmesoftware.acmesuite;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the module boundaries of the modular monolith (Spring Modulith): no illegal
 * accesses to internal packages of other modules.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(AcmeSuiteApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        modules.forEach(System.out::println);
    }
}
