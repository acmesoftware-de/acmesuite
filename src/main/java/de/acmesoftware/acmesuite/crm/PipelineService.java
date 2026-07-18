package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.CrmViews.DealView;
import de.acmesoftware.acmesuite.crm.domain.Deal;
import de.acmesoftware.acmesuite.crm.domain.DealRepository;
import de.acmesoftware.acmesuite.crm.domain.DealSource;
import de.acmesoftware.acmesuite.crm.domain.PipelineStage;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ACMEcrm sales pipeline overlay (see api/acme-crm.yaml, Pipeline). The deal is the source of
 * truth for the settable stage; the (documented) live projection from quotes/orders is future work.
 */
@Service
@Transactional
public class PipelineService {

    private final DealRepository deals;

    public PipelineService(DealRepository deals) {
        this.deals = deals;
    }

    @Transactional(readOnly = true)
    public List<DealView> listPipeline(String customerId, String contactId, PipelineStage stage, String owner,
                                       String q) {
        String needle = q == null ? null : q.toLowerCase();
        List<Deal> base = customerId != null ? deals.findByCustomerId(customerId)
                : contactId != null ? deals.findByContactId(contactId)
                : deals.findAll();
        return base.stream()
                .filter(d -> stage == null || d.getStage() == stage)
                .filter(d -> owner == null || owner.equalsIgnoreCase(d.getOwnerInitials()))
                .filter(d -> needle == null
                        || d.getCompany().toLowerCase().contains(needle)
                        || (d.getContact() != null && d.getContact().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing(PipelineService::amount).reversed())
                .map(DealView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DealView> getDeal(String id) {
        return deals.findById(id).map(DealView::of);
    }

    public DealView createDeal(String company, String contact, String customerId, String contactId,
                               PipelineStage stage, Money value, String ownerInitials) {
        if (company == null || company.isBlank()) {
            throw unprocessable("company is required");
        }
        PipelineStage s = stage == null ? PipelineStage.NEU : stage;
        String id = "deal-" + UUID.randomUUID().toString().substring(0, 12);
        Deal d = new Deal(id, DealSource.LEAD, customerId, contactId, company, contact, s, ownerInitials, null,
                value, null, null, "Neuer Lead", null, LocalDate.now());
        return DealView.of(deals.save(d));
    }

    public DealView updateDeal(String id, PipelineStage stage, String company, String contact, Money value,
                               String ownerInitials, String contactId) {
        Deal d = deals.findById(id).orElseThrow(() -> notFound("Deal " + id + " unknown"));
        d.update(stage, company, contact, value, ownerInitials, contactId, LocalDate.now());
        return DealView.of(d);
    }

    private static BigDecimal amount(Deal d) {
        return d.getValue() == null || d.getValue().amount() == null ? BigDecimal.ZERO : d.getValue().amount();
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException unprocessable(String msg) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
    }
}
