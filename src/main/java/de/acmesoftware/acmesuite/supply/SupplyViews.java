package de.acmesoftware.acmesuite.supply;

import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import de.acmesoftware.acmesuite.supply.domain.Material;
import de.acmesoftware.acmesuite.supply.domain.Supplier;
import de.acmesoftware.acmesuite.supply.domain.Tier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Serializable ACMEsupply views (DTOs) — congruent with {@code api/acme-supply.yaml}. */
public final class SupplyViews {

    private SupplyViews() {
    }

    public record MoneyView(BigDecimal amount, String currency, boolean unlimited) {
        public static MoneyView of(Money m) {
            boolean unl = m == null || m.isUnlimited();
            return new MoneyView(unl ? null : m.amount(), unl ? null : m.currency(), unl);
        }
    }

    public record SupplierView(String id, String name, String status, String email, String country) {
        public static SupplierView of(Supplier s) {
            return new SupplierView(s.getId(), s.getName(), s.getStatus().name(), s.getEmail(), s.getCountry());
        }
    }

    public record MaterialView(String id, String code, String name, String kind, String unit) {
        public static MaterialView of(Material m) {
            return new MaterialView(m.getId(), m.getCode(), m.getName(), m.getKind().name(), m.getUnit());
        }
    }

    public record TierView(int minQuantity, BigDecimal unitPrice) {
        public static TierView of(Tier t) {
            return new TierView(t.getMinQuantity(), t.getUnitPrice());
        }
    }

    public record StockView(String materialId, String materialName, BigDecimal quantity,
                            BigDecimal reorderLevel, boolean belowReorder) {
        public static StockView of(de.acmesoftware.acmesuite.supply.domain.MaterialStock s, String name) {
            return new StockView(s.getMaterialId(), name, s.getQuantity(), s.getReorderLevel(), s.belowReorder());
        }
    }

    public record SupplyContractView(String id, String supplierId, String supplierName, String materialId,
                                     String materialName, String currency, int leadTimeDays, DateRange validity,
                                     List<TierView> tiers) {
    }

    public record ResolvedSupplyPriceView(String supplierId, String materialId, int quantity, BigDecimal unitPrice,
                                          String currency, int leadTimeDays, String contractId) {
    }

    public record LineView(String materialId, String materialName, int quantity, BigDecimal unitPrice,
                           BigDecimal lineTotal) {
    }

    public record ApprovalView(boolean required, String approverId, String decision, LocalDate decidedOn,
                               String comment) {
    }

    public record SupplyOrderView(String id, String supplierId, String supplierName, String status,
                                  LocalDate orderDate, LocalDate expectedDeliveryDate, List<LineView> lines,
                                  MoneyView total, ApprovalView approval, String note, String deliveryMode) {
    }
}
