package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.domain.Contact;
import de.acmesoftware.acmesuite.crm.domain.Customer;
import de.acmesoftware.acmesuite.crm.domain.Deal;
import de.acmesoftware.acmesuite.crm.domain.MailMessage;
import de.acmesoftware.acmesuite.crm.domain.MailThread;
import de.acmesoftware.acmesuite.crm.domain.PriceList;
import de.acmesoftware.acmesuite.crm.domain.PriceListItem;
import de.acmesoftware.acmesuite.crm.domain.Product;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Serializable ACMEcrm views (DTOs) — congruent with {@code api/acme-crm.yaml}. */
public final class CrmViews {

    private CrmViews() {
    }

    public record MoneyView(BigDecimal amount, String currency, boolean unlimited) {
        public static MoneyView of(Money m) {
            boolean unl = m == null || m.isUnlimited();
            return new MoneyView(unl ? null : m.amount(), unl ? null : m.currency(), unl);
        }
    }

    public record CustomerView(String id, String name, String kind, String status, String email, String country,
                               String parentResellerId, String priceListId) {
        public static CustomerView of(Customer c) {
            return new CustomerView(c.getId(), c.getName(), c.getKind().name(), c.getStatus().name(),
                    c.getEmail(), c.getCountry(), c.getParentResellerId(), c.getPriceListId());
        }
    }

    public record ProductView(String id, String sku, String name, String category, String unit, boolean active,
                              MoneyView listPrice) {
        public static ProductView of(Product p) {
            return new ProductView(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getUnit(),
                    p.isActive(), MoneyView.of(p.getListPrice()));
        }
    }

    public record PriceListItemView(String productId, BigDecimal unitPrice, int minQuantity) {
        public static PriceListItemView of(PriceListItem i) {
            return new PriceListItemView(i.getProductId(), i.getUnitPrice(), i.getMinQuantity());
        }
    }

    public record PriceListView(String id, String name, String currency, String kind, DateRange validity,
                                List<PriceListItemView> items) {
        public static PriceListView of(PriceList p) {
            return new PriceListView(p.getId(), p.getName(), p.getCurrency(), p.getKind().name(), p.getValidity(),
                    p.getItems().stream().map(PriceListItemView::of).toList());
        }
    }

    public record ResolvedPriceView(String productId, String customerId, int quantity, BigDecimal unitPrice,
                                    String currency, String priceListId, String source) {
    }

    public record LineView(String productId, String productName, int quantity, BigDecimal unitPrice,
                           BigDecimal discountPercent, BigDecimal lineTotal, int remainingQuantity) {
    }

    public record QuoteView(String id, String customerId, String customerName, String status, String currency,
                            java.time.LocalDate validUntil, List<LineView> lines, BigDecimal netTotal,
                            java.time.LocalDate createdOn) {
    }

    public record ApprovalView(boolean required, String approverId, String decision,
                               java.time.LocalDate decidedOn, String comment) {
    }

    public record OrderView(String id, String customerId, String customerName, String customerCountry,
                            String quoteId, String status,
                            java.time.LocalDate orderDate, List<LineView> lines, MoneyView total,
                            ApprovalView approval, String note, String shippingMode) {
    }

    // ── Contacts / Pipeline / Mail (see api/acme-crm.yaml) ──

    public record ContactView(String id, String customerId, String name, String role, String email, String phone,
                              boolean primary, boolean newsletter) {
        public static ContactView of(Contact c) {
            return new ContactView(c.getId(), c.getCustomerId(), c.getName(), c.getRole(), c.getEmail(),
                    c.getPhone(), c.isPrimary(), c.isNewsletter());
        }
    }

    public record OwnerView(String initials, String name) {
    }

    public record DealView(String id, String source, String quoteId, String orderId, String customerId,
                           String contactId, String company, String contact, String stage, int probability,
                           MoneyView value, OwnerView owner, String lastActivity, Instant lastActivityAt,
                           Integer ageDays) {
        public static DealView of(Deal d) {
            OwnerView owner = d.getOwnerInitials() == null ? null
                    : new OwnerView(d.getOwnerInitials(), d.getOwnerName());
            Integer age = d.getStageSince() == null ? null
                    : (int) ChronoUnit.DAYS.between(d.getStageSince(), LocalDate.now());
            return new DealView(d.getId(), d.getSource().name(), d.getQuoteId(), d.getOrderId(), d.getCustomerId(),
                    d.getContactId(), d.getCompany(), d.getContact(), d.getStage().name(), d.getStage().probability(),
                    MoneyView.of(d.getValue()), owner, d.getLastActivity(), d.getLastActivityAt(), age);
        }
    }

    public record MailMessageView(String id, String direction, String from, String to, Instant sentAt,
                                  String snippet, String body) {
        public static MailMessageView of(MailMessage m) {
            return new MailMessageView(m.getMessageId(), m.getDirection().name(), m.getFromAddr(), m.getToAddr(),
                    m.getSentAt(), m.getSnippet(), m.getBody());
        }
    }

    public record MailThreadView(String id, String subject, String customerId, String contactId,
                                 List<String> participants, int messageCount, Instant lastMessageAt, String preview,
                                 List<MailMessageView> messages) {
        public static MailThreadView of(MailThread t) {
            List<MailMessageView> msgs = t.getMessages().stream().map(MailMessageView::of).toList();
            String preview = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).body();
            return new MailThreadView(t.getId(), t.getSubject(), t.getCustomerId(), t.getContactId(),
                    List.copyOf(t.getParticipants()), msgs.size(), t.getLastMessageAt(), preview, msgs);
        }
    }
}
