package de.acmesoftware.acmesuite.crm.web;

import de.acmesoftware.acmesuite.crm.CrmViews.DealView;
import de.acmesoftware.acmesuite.crm.PipelineService;
import de.acmesoftware.acmesuite.crm.domain.PipelineStage;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ACMEcrm sales pipeline overlay (see api/acme-crm.yaml, Pipeline). */
@RestController
@RequestMapping("/api/crm")
public class PipelineController {

    private final PipelineService pipeline;

    public PipelineController(PipelineService pipeline) {
        this.pipeline = pipeline;
    }

    @GetMapping("/pipeline")
    public List<DealView> pipeline(
            @RequestParam(required = false) PipelineStage stage,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String contactId,
            @RequestParam(required = false) String q) {
        return pipeline.listPipeline(customerId, contactId, stage, owner, q);
    }

    @GetMapping("/pipeline/{id}")
    public ResponseEntity<DealView> deal(@PathVariable String id) {
        return ResponseEntity.of(pipeline.getDeal(id));
    }

    @PostMapping("/pipeline")
    public ResponseEntity<DealView> createDeal(@RequestBody DealCreate req) {
        DealView v = pipeline.createDeal(req.company(), req.contact(), req.customerId(), req.contactId(),
                req.stage(), money(req.value()), req.ownerInitials());
        return ResponseEntity.created(URI.create("/api/crm/pipeline/" + v.id())).body(v);
    }

    @PatchMapping("/pipeline/{id}")
    public DealView updateDeal(@PathVariable String id, @RequestBody DealUpdate req) {
        return pipeline.updateDeal(id, req.stage(), req.company(), req.contact(), money(req.value()),
                req.ownerInitials(), req.contactId());
    }

    private static Money money(MoneyInput m) {
        if (m == null || Boolean.TRUE.equals(m.unlimited()) || m.amount() == null) {
            return null;
        }
        return new Money(m.amount(), m.currency() == null ? "EUR" : m.currency());
    }

    /** Lenient money request body: {@code unlimited} is optional (unlike the primitive in MoneyView). */
    public record MoneyInput(BigDecimal amount, String currency, Boolean unlimited) {
    }

    public record DealCreate(String company, String contact, String customerId, String contactId,
                             PipelineStage stage, MoneyInput value, String ownerInitials) {
    }

    public record DealUpdate(PipelineStage stage, String company, String contact, String contactId,
                             MoneyInput value, String ownerInitials) {
    }
}
