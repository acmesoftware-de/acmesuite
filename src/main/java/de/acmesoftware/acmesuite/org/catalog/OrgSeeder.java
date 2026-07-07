package de.acmesoftware.acmesuite.org.catalog;

import de.acmesoftware.acmesuite.org.catalog.AcmeOrgCatalog.PersonSpec;
import de.acmesoftware.acmesuite.org.catalog.AcmeOrgCatalog.UnitSpec;
import de.acmesoftware.acmesuite.org.domain.Absence;
import de.acmesoftware.acmesuite.org.domain.AbsenceRepository;
import de.acmesoftware.acmesuite.org.domain.CompensationType;
import de.acmesoftware.acmesuite.org.domain.CostCenter;
import de.acmesoftware.acmesuite.org.domain.CostCenterRepository;
import de.acmesoftware.acmesuite.org.domain.LegalEntity;
import de.acmesoftware.acmesuite.org.domain.LegalEntityRepository;
import de.acmesoftware.acmesuite.org.domain.LegalEntityType;
import de.acmesoftware.acmesuite.org.domain.OrgUnit;
import de.acmesoftware.acmesuite.org.domain.OrgUnitRepository;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.ApprovalLimit;
import de.acmesoftware.acmesuite.org.domain.ApprovalLimitRepository;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorney;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyRepository;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyType;
import de.acmesoftware.acmesuite.org.domain.SignatureRule;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the canonical ACME org from the {@link AcmeOrgCatalog} into the database (idempotent:
 * only when empty). This makes this the system of record for the organization — the same structure that
 * an external contract-management platform later projects via the org-source connector.
 *
 * <p>The schema belongs to Flyway; this runner only fills in data. Two passes, because reporting
 * line and deputy can only be resolved once all persons exist.
 */
@Component
@Order(0)
public class OrgSeeder implements ApplicationRunner {

    private final LegalEntityRepository legalEntities;
    private final OrgUnitRepository orgUnits;
    private final PersonRepository persons;
    private final AbsenceRepository absences;
    private final CostCenterRepository costCenters;
    private final PowerOfAttorneyRepository powersOfAttorney;
    private final ApprovalLimitRepository approvalLimits;

    public OrgSeeder(LegalEntityRepository legalEntities, OrgUnitRepository orgUnits,
                     PersonRepository persons, AbsenceRepository absences,
                     CostCenterRepository costCenters, PowerOfAttorneyRepository powersOfAttorney,
                     ApprovalLimitRepository approvalLimits) {
        this.legalEntities = legalEntities;
        this.orgUnits = orgUnits;
        this.persons = persons;
        this.absences = absences;
        this.costCenters = costCenters;
        this.powersOfAttorney = powersOfAttorney;
        this.approvalLimits = approvalLimits;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (persons.count() > 0) {
            return; // already seeded
        }
        var catalog = new AcmeOrgCatalog();

        LegalEntity acme = legalEntities.save(new LegalEntity(
                AcmeOrgCatalog.ROOT_LEGAL_ENTITY, AcmeOrgCatalog.ROOT_LEGAL_NAME,
                LegalEntityType.HOLDING, null, "DE", "HRB 100001"));
        acme.mapToPlatformTenant("acme-group");
        legalEntities.save(acme);

        // Units (the catalog delivers them parent-before-child).
        Map<String, OrgUnit> unitByKey = new HashMap<>();
        for (UnitSpec u : catalog.units()) {
            OrgUnit parent = u.parentKey() == null ? null : unitByKey.get(u.parentKey());
            unitByKey.put(u.key(), orgUnits.save(new OrgUnit(u.key(), u.name(), u.type(), acme, parent)));
        }

        // Pass 1: persons without overlay.
        Map<String, Person> personByKey = new HashMap<>();
        for (PersonSpec p : catalog.persons()) {
            String[] name = splitName(p.displayName());
            Person person = persons.save(new Person(p.key(), name[0], name[1], p.email(), p.title(),
                    unitByKey.get(p.unitKey())));
            personByKey.put(p.key(), person);
        }

        // Pass 2: reporting line, deputy, assistant, matrix + compensation (default).
        for (PersonSpec p : catalog.persons()) {
            Person person = personByKey.get(p.key());
            if (p.managerKey() != null) {
                person.assignManager(personByKey.get(p.managerKey()));
            }
            p.delegateKeys().forEach(person::addDelegate);
            p.assistantKeys().forEach(person::addAssistant);
            person.setCompensation(defaultCompType(p), defaultRate(p));
            person.setApplicant(p.applicant());
            persons.save(person);
        }
        catalog.secondaryMemberships().forEach(m -> {
            Person person = personByKey.get(m.personKey());
            person.addSecondaryUnit(m.unitKey());
            persons.save(person);
        });

        // Absences with deputy.
        int absSeq = 1;
        for (var a : catalog.absences()) {
            absences.save(new Absence("abs-" + (absSeq++), personByKey.get(a.personKey()), a.reasonKey(),
                    a.substituteKey() == null ? null : personByKey.get(a.substituteKey()),
                    new DateRange(a.from(), a.until())));
        }

        seedAuthorityAndCostCenters(acme, unitByKey, personByKey);
    }

    /** Lean, canonical powers of attorney + cost centers — the basis for the later approval logic. */
    private void seedAuthorityAndCostCenters(LegalEntity acme, Map<String, OrgUnit> units,
                                             Map<String, Person> people) {
        LocalDate since = LocalDate.of(2018, 1, 1);
        powersOfAttorney.save(new PowerOfAttorney("poa-gf-1", people.get("u-gf-1"), acme,
                PowerOfAttorneyType.GENERAL, SignatureRule.SOLE, Money.unlimited(),
                "Generalvollmacht Geschäftsführung", DateRange.openFrom(since)));
        powersOfAttorney.save(new PowerOfAttorney("poa-gf-2", people.get("u-gf-2"), acme,
                PowerOfAttorneyType.GENERAL, SignatureRule.SOLE, Money.unlimited(),
                "Generalvollmacht Geschäftsführung", DateRange.openFrom(since)));
        powersOfAttorney.save(new PowerOfAttorney("poa-cfo", people.get("u-finance-cfo"), acme,
                PowerOfAttorneyType.PROKURA, SignatureRule.SOLE, Money.unlimited(),
                "Einzelprokura Finanzen", DateRange.openFrom(since)));
        powersOfAttorney.save(new PowerOfAttorney("poa-einkauf", people.get("u-einkauf-lead"), acme,
                PowerOfAttorneyType.HANDLUNGSVOLLMACHT, SignatureRule.SOLE, Money.euro("250000"),
                "Einkauf bis 250k EUR", DateRange.openFrom(since)));

        // Flat internal approval limits per role (global, explicit → overrides the power-of-attorney derivation).
        DateRange validity = DateRange.openFrom(since);
        // Team leads: 50k
        for (String teamLead : List.of("u-a1-1-1", "u-a1-1-4", "u-b1-1-1", "u-b1-2-8",
                "u-b2-1-8", "u-b2-2-8", "u-b2-3-1")) {
            approvalLimits.save(new ApprovalLimit("al-" + teamLead, people.get(teamLead), null,
                    Money.euro("50000"), validity));
        }
        // Department and division heads: 100k
        for (String lead : List.of("u-abt-a1-lead", "u-abt-b1-lead", "u-abt-b2-lead",
                "u-fb-a-lead", "u-fb-b-lead")) {
            approvalLimits.save(new ApprovalLimit("al-" + lead, people.get(lead), null,
                    Money.euro("100000"), validity));
        }
        // Controlling head: 250k (above this the CFO must sign).
        approvalLimits.save(new ApprovalLimit("al-controlling-lead", people.get("u-controlling-lead"), null,
                Money.euro("250000"), validity));
        // CFO: 500k (above this the management must sign).
        approvalLimits.save(new ApprovalLimit("al-cfo", people.get("u-finance-cfo"), null,
                Money.euro("500000"), validity));

        costCenters.save(new CostCenter("CC-EINKAUF", "Einkauf", units.get("ou-einkauf"),
                people.get("u-einkauf-lead"), Money.euro("1200000")));
        costCenters.save(new CostCenter("CC-FINANCE", "Finance", units.get("ou-finance"),
                people.get("u-finance-cfo"), Money.euro("300000")));
        costCenters.save(new CostCenter("CC-FB-A", "Fachbereich A", units.get("ou-fb-a"),
                people.get("u-fb-a-lead"), Money.euro("2000000")));
        costCenters.save(new CostCenter("CC-FB-B", "Fachbereich B", units.get("ou-fb-b"),
                people.get("u-fb-b-lead"), Money.euro("5000000")));
    }

    private static String[] splitName(String displayName) {
        int sp = displayName.indexOf(' ');
        return sp < 0 ? new String[]{displayName, ""} : new String[]{
                displayName.substring(0, sp), displayName.substring(sp + 1)};
    }

    /** Management (without a superior) and leads (-lead/-cfo) → salary; everyone else → hourly wage. */
    private static CompensationType defaultCompType(PersonSpec p) {
        return isGf(p) || isLead(p) ? CompensationType.SALARIED : CompensationType.HOURLY;
    }

    private static BigDecimal defaultRate(PersonSpec p) {
        if (isGf(p)) {
            return new BigDecimal("50.00");
        }
        return isLead(p) ? new BigDecimal("35.00") : new BigDecimal("25.00");
    }

    private static boolean isGf(PersonSpec p) {
        return p.managerKey() == null;
    }

    private static boolean isLead(PersonSpec p) {
        return p.key().endsWith("-lead") || p.key().endsWith("-cfo");
    }
}
