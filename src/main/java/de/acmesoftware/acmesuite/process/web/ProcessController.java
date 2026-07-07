package de.acmesoftware.acmesuite.process.web;

import de.acmesoftware.acmesuite.process.ContractView;
import de.acmesoftware.acmesuite.process.ProcessEngine;
import de.acmesoftware.acmesuite.process.ProcessEngine.ArchivedContract;
import de.acmesoftware.acmesuite.process.ProcessEngine.ProcessStats;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read access to the contract pipeline of "ACME offline". */
@RestController
@RequestMapping("/api/process")
class ProcessController {

    private final ProcessEngine engine;

    ProcessController(ProcessEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/contracts")
    List<ContractView> contracts() {
        return engine.activeContracts();
    }

    @GetMapping("/archive")
    List<ArchivedContract> archive() {
        return engine.archivedContracts();
    }

    @GetMapping("/stats")
    ProcessStats stats() {
        return engine.stats();
    }
}
