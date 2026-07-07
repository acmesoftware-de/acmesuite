package de.acmesoftware.acmesuite.org.catalog;

import de.acmesoftware.acmesuite.org.domain.OrgUnitType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generator of the sample company "ACME Group GmbH" — the canonical org source of the system.
 *
 * <p>Structurally identical to a reference company catalog (shared truth): two managing
 * directors (each with an assistant, division split via managerKey), Fachbereich A (one department
 * with two 4-person teams), Fachbereich B (two departments with a shared assistant, each with three
 * 8-person teams and one deputy each), the Einkauf/Legal/Compliance/Finance/Controlling functions,
 * plus a data-protection committee (matrix). The name/email algorithm is deliberately identical — so
 * this system and the external platform know the same 88 persons with the same addresses ({@code v.lastname@acme-group.io}).
 */
public final class AcmeOrgCatalog {

    public record UnitSpec(String key, String name, OrgUnitType type, String parentKey) {
    }

    public record PersonSpec(String key, String displayName, String email, String unitKey,
                             String title, String managerKey, List<String> delegateKeys,
                             List<String> assistantKeys, boolean applicant) {
    }

    public record SecondaryMembership(String personKey, String unitKey) {
    }

    public record AbsenceSpec(String personKey, String reasonKey, String substituteKey,
                              LocalDate from, LocalDate until) {
    }

    /** Root legal entity: the single legal person to which all units belong. */
    public static final String ROOT_LEGAL_ENTITY = "acme";
    public static final String ROOT_LEGAL_NAME = "ACME Group GmbH";

    private static final String[] FIRST = {
            "Anna", "Bernd", "Clara", "David", "Eva", "Florian", "Greta", "Hans", "Ina", "Jens",
            "Katrin", "Lars", "Maria", "Nils", "Olga", "Paul", "Quirin", "Rita", "Sven", "Tanja",
            "Udo", "Vera", "Werner", "Xenia", "Yannick", "Zoe"};
    private static final String[] LAST = {
            "Albrecht", "Brandt", "Conrad", "Dietrich", "Engel", "Frey", "Götz", "Huber", "Iversen",
            "Jung", "Kühn", "Lorenz", "Möller", "Neumann", "Otto", "Pfeiffer", "Richter", "Schmitt",
            "Thiel", "Ulrich", "Voss", "Wagner", "Zimmermann"};

    /**
     * Fixed names per key (ACME roster). The first 88 are the canonical workforce; the 11 positions
     * added for "ACME offline" (procurement assistant, sales, HR) get names from the reserve pool.
     * If a key is missing, the generic FIRST/LAST generator kicks in as a fallback.
     */
    private static final Map<String, String> NAMES = new java.util.HashMap<>();

    static {
        // Management
        NAMES.put("u-gf-1", "Julia Jefa");
        NAMES.put("u-gf-1-asst", "Anton Assistent");
        NAMES.put("u-gf-2", "Betty Boss");
        NAMES.put("u-gf-2-asst", "Armin Assistent");
        // Fachbereich A
        NAMES.put("u-fb-a-lead", "Felix Fachmann");
        NAMES.put("u-abt-a1-lead", "Dirk Direktor");
        NAMES.put("u-abt-a1-asst", "Anja Assistentin");
        NAMES.put("u-a1-1-1", "Mia Managerin");
        NAMES.put("u-a1-1-2", "Paul Profi");
        NAMES.put("u-a1-1-3", "Stefan Spezialist");
        NAMES.put("u-a1-1-4", "Tina Talent");
        NAMES.put("u-a1-2-1", "Mark Mentor");
        NAMES.put("u-a1-2-2", "Clara Clever");
        NAMES.put("u-a1-2-3", "Ralf Routinier");
        NAMES.put("u-a1-2-4", "Petra Perfekt");
        // Fachbereich B – leadership
        NAMES.put("u-fb-b-lead", "Franziska Führungskraft");
        NAMES.put("u-abt-b1-lead", "Bastian Bereichsleiter");
        NAMES.put("u-abt-b2-lead", "Diana Direktorin");
        NAMES.put("u-fb-b-asst", "Sabine Support");
        // Team B1-1 (u-b1-1-1 is the deputy/delegate)
        NAMES.put("u-b1-1-1", "Thomas Teamleiter");
        NAMES.put("u-b1-1-2", "Monika Macherin");
        NAMES.put("u-b1-1-3", "Kevin Kenner");
        NAMES.put("u-b1-1-4", "Lisa Leistung");
        NAMES.put("u-b1-1-5", "Norbert Navigator");
        NAMES.put("u-b1-1-6", "Paula Praktikerin");
        NAMES.put("u-b1-1-7", "Rita Resultat");
        NAMES.put("u-b1-1-8", "Viktor Visionär");
        // Team B1-2
        NAMES.put("u-b1-2-1", "Daniel Delegierter");
        NAMES.put("u-b1-2-2", "Bianca Beraterin");
        NAMES.put("u-b1-2-3", "Carsten Champion");
        NAMES.put("u-b1-2-4", "Eva Expertin");
        NAMES.put("u-b1-2-5", "Gerd Gestalter");
        NAMES.put("u-b1-2-6", "Helga Helferin");
        NAMES.put("u-b1-2-7", "Jonas Joker");
        NAMES.put("u-b1-2-8", "Karin Könnerin");
        // Team B1-3
        NAMES.put("u-b1-3-1", "Tobias Teamsprecher");
        NAMES.put("u-b1-3-2", "Marlene Meisterin");
        NAMES.put("u-b1-3-3", "Nils Navigator");
        NAMES.put("u-b1-3-4", "Olaf Organisator");
        NAMES.put("u-b1-3-5", "Patricia Profi");
        NAMES.put("u-b1-3-6", "Rüdiger Rechner");
        NAMES.put("u-b1-3-7", "Silke Strategin");
        NAMES.put("u-b1-3-8", "Werner Wegbereiter");
        // Team B2-1
        NAMES.put("u-b2-1-1", "Dennis Delegierter");
        NAMES.put("u-b2-1-2", "Britta Brillanz");
        NAMES.put("u-b2-1-3", "Christian Coach");
        NAMES.put("u-b2-1-4", "Elena Expertin");
        NAMES.put("u-b2-1-5", "Falk Fachkraft");
        NAMES.put("u-b2-1-6", "Greta Gestalterin");
        NAMES.put("u-b2-1-7", "Hannes Helfer");
        NAMES.put("u-b2-1-8", "Inga Impuls");
        // Team B2-2
        NAMES.put("u-b2-2-1", "Tim Teamvertreter");
        NAMES.put("u-b2-2-2", "Mona Motivatorin");
        NAMES.put("u-b2-2-3", "Nico Netzwerker");
        NAMES.put("u-b2-2-4", "Oliver Optimierer");
        NAMES.put("u-b2-2-5", "Pia Planerin");
        NAMES.put("u-b2-2-6", "Rainer Realist");
        NAMES.put("u-b2-2-7", "Sven Spezialist");
        NAMES.put("u-b2-2-8", "Walter Wegweiser");
        // Team B2-3
        NAMES.put("u-b2-3-1", "David Delegat");
        NAMES.put("u-b2-3-2", "Beate Beraterin");
        NAMES.put("u-b2-3-3", "Claudia Controllerin");
        NAMES.put("u-b2-3-4", "Erik Entwickler");
        NAMES.put("u-b2-3-5", "Frank Förderer");
        NAMES.put("u-b2-3-6", "Gudrun Garantin");
        NAMES.put("u-b2-3-7", "Heiko Helfer");
        NAMES.put("u-b2-3-8", "Ivonne Impulsgeberin");
        // Procurement
        NAMES.put("u-einkauf-lead", "Hannah Handel");
        NAMES.put("u-einkauf-1", "Egon Einkauf");
        NAMES.put("u-einkauf-2", "Petra Procurement");
        NAMES.put("u-einkauf-3", "Konrad Käufer");
        // Legal department
        NAMES.put("u-legal-lead", "Rainer Recht");
        NAMES.put("u-legal-1", "Laura Legal");
        NAMES.put("u-legal-2", "Lena Law");
        // Compliance
        NAMES.put("u-compliance-lead", "Claudia Compliance");
        NAMES.put("u-compliance-1", "Moritz Maßgabe");
        NAMES.put("u-compliance-2", "Petra Policy");
        NAMES.put("u-compliance-3", "Richard Regelwerk");
        // Finance
        NAMES.put("u-finance-cfo", "Friedrich Finance");
        NAMES.put("u-finance-1", "Fabian Forecast");
        NAMES.put("u-finance-2", "Miriam Marge");
        NAMES.put("u-finance-3", "Nadine Netto");
        NAMES.put("u-finance-4", "Thomas Treasury");
        // Controlling
        NAMES.put("u-controlling-lead", "Karl Kontrolle");
        NAMES.put("u-controlling-1", "Carla Cashflow");
        NAMES.put("u-controlling-2", "Bettina Budget");
        NAMES.put("u-controlling-3", "Dirk Datenanalyse");
        NAMES.put("u-controlling-4", "Stefan Soll-Ist");
        // "ACME offline" additions → reserve pool
        NAMES.put("u-einkauf-asst", "Alfred Analyse");
        NAMES.put("u-vertrieb-lead", "Bruno Benchmark");
        NAMES.put("u-vertrieb-1", "Celine Compliance");
        NAMES.put("u-vertrieb-2", "Dieter Dokumentation");
        NAMES.put("u-vertrieb-3", "Elke Effizienz");
        NAMES.put("u-vertrieb-4", "Fabienne Forecast");
        NAMES.put("u-vertrieb-asst", "Günter Governance");
        NAMES.put("u-hr-lead", "Heidi Harmonisierung");
        NAMES.put("u-hr-1", "Jörg Jahresabschluss");
        NAMES.put("u-hr-2", "Kerstin KPI");
        NAMES.put("u-hr-asst", "Lothar Liquidität");
    }

    private final List<UnitSpec> units = new ArrayList<>();
    private final List<PersonSpec> persons = new ArrayList<>();
    private final List<SecondaryMembership> secondaryMemberships = new ArrayList<>();
    private final List<AbsenceSpec> absences = new ArrayList<>();
    private final Set<String> usedEmails = new HashSet<>();
    private int nameSeq;

    public AcmeOrgCatalog() {
        build();
    }

    public List<UnitSpec> units() {
        return List.copyOf(units);
    }

    public List<PersonSpec> persons() {
        return List.copyOf(persons);
    }

    public List<SecondaryMembership> secondaryMemberships() {
        return List.copyOf(secondaryMemberships);
    }

    public List<AbsenceSpec> absences() {
        return List.copyOf(absences);
    }

    // ---- Aufbau ----

    private void build() {
        unit("ou-acme", ROOT_LEGAL_NAME, OrgUnitType.DIVISION, null);
        unit("ou-gf", "Geschäftsführung", OrgUnitType.DIVISION, "ou-acme");

        person("u-gf-1", "ou-gf", "Geschäftsführerin", null, List.of(), List.of("u-gf-1-asst"));
        person("u-gf-2", "ou-gf", "Geschäftsführerin", null, List.of(), List.of("u-gf-2-asst"));
        person("u-gf-1-asst", "ou-gf", "Assistenz der Geschäftsführung", "u-gf-1", List.of(), List.of(), true);
        person("u-gf-2-asst", "ou-gf", "Assistenz der Geschäftsführung", "u-gf-2", List.of(), List.of(), true);

        buildFachbereichA();
        buildFachbereichB();
        buildFunctions();
        buildSalesAndHr();
        buildMatrix();
    }

    private void buildMatrix() {
        unit("ou-gremium", "Gremium Datenschutz", OrgUnitType.DEPARTMENT, "ou-gf");
        secondaryMemberships.add(new SecondaryMembership("u-compliance-1", "ou-gremium"));
        secondaryMemberships.add(new SecondaryMembership("u-finance-1", "ou-gremium"));

        absences.add(new AbsenceSpec("u-abt-b1-lead", "urlaub-1", "u-b1-1-1",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15)));
        absences.add(new AbsenceSpec("u-gf-1", "kur-1", "u-gf-1-asst",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)));
    }

    private void buildFachbereichA() {
        unit("ou-fb-a", "Fachbereich A", OrgUnitType.DIVISION, "ou-gf");
        person("u-fb-a-lead", "ou-fb-a", "Bereichsleitung", "u-gf-2", List.of(), List.of());

        unit("ou-abt-a1", "Abteilung A1", OrgUnitType.DEPARTMENT, "ou-fb-a");
        List<String> members = new ArrayList<>();
        for (int t = 1; t <= 2; t++) {
            for (int m = 1; m <= 4; m++) members.add("u-a1-" + t + "-" + m);
        }
        List<String> leads = List.of("u-a1-1-1", "u-a1-1-4"); // Mia Managerin, Tina Talent
        person("u-abt-a1-lead", "ou-abt-a1", "Abteilungsleitung", "u-fb-a-lead", leads, List.of("u-abt-a1-asst"));
        person("u-abt-a1-asst", "ou-abt-a1", "Assistenz", "u-abt-a1-lead", List.of(), List.of(), true);
        teamLayer("a1", "u-abt-a1-lead", "A1", leads, members);
    }

    private void buildFachbereichB() {
        unit("ou-fb-b", "Fachbereich B", OrgUnitType.DIVISION, "ou-gf");
        person("u-fb-b-lead", "ou-fb-b", "Bereichsleitung", "u-gf-2", List.of(), List.of());
        person("u-fb-b-asst", "ou-fb-b", "Assistenz (geteilt)", "u-fb-b-lead", List.of(), List.of(), true);

        // Team leads per department: they report to the department head, the members report to them.
        List<List<String>> leadsByAbt = List.of(
                List.of("u-b1-1-1", "u-b1-2-8"),             // Thomas Teamleiter, Karin Könnerin
                List.of("u-b2-1-8", "u-b2-2-8", "u-b2-3-1")  // Inga Impuls, Walter Wegweiser, David Delegat
        );
        for (int a = 1; a <= 2; a++) {
            String abt = "ou-abt-b" + a;
            String abtLead = "u-abt-b" + a + "-lead";
            unit(abt, "Abteilung B" + a, OrgUnitType.DEPARTMENT, "ou-fb-b");
            List<String> members = new ArrayList<>();
            for (int t = 1; t <= 3; t++) {
                for (int m = 1; m <= 8; m++) members.add("u-b" + a + "-" + t + "-" + m);
            }
            List<String> leads = leadsByAbt.get(a - 1);
            person(abtLead, abt, "Abteilungsleitung", "u-fb-b-lead", leads, List.of("u-fb-b-asst"));
            teamLayer("b" + a, abtLead, "B" + a, leads, members);
        }
    }

    /**
     * Team-lead layer between the department head and the staff: one team unit per lead; the lead
     * reports to the department head, and the remaining members are distributed round-robin evenly
     * across the leads.
     */
    /** Team leads whose team (including the lead) starts as applicants (not hired). */
    private static final Set<String> NOT_HIRED_TEAMS = Set.of("u-a1-1-4", "u-b2-2-8", "u-b2-3-1"); // Tina, Walter, David
    /** At most this many hired members per team; the rest start as applicants. */
    private static final int HIRED_PER_TEAM = 1; // only 1 member per team hired, the second is an applicant

    private void teamLayer(String abtKey, String abtLeadKey, String abtLabel,
                           List<String> leadKeys, List<String> memberKeys) {
        int n = leadKeys.size();
        List<String> teamUnits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String tu = "ou-team-" + abtKey + "-" + (i + 1);
            unit(tu, "Team " + abtLabel + "." + (i + 1), OrgUnitType.TEAM, "ou-abt-" + abtKey);
            teamUnits.add(tu);
            person(leadKeys.get(i), tu, "Teamleitung", abtLeadKey, List.of(), List.of(),
                    NOT_HIRED_TEAMS.contains(leadKeys.get(i)));
        }
        int idx = 0;
        int[] hired = new int[n];
        for (String m : memberKeys) {
            if (leadKeys.contains(m)) {
                continue;
            }
            int t = idx++ % n;
            String lead = leadKeys.get(t);
            // Applicant: team not hired OR beyond the hiring limit.
            boolean applicant = NOT_HIRED_TEAMS.contains(lead) || hired[t]++ >= HIRED_PER_TEAM;
            person(m, teamUnits.get(t), "Teammitglied", lead, List.of(), List.of(), applicant);
        }
    }

    private void buildFunctions() {
        unit("ou-einkauf", "Einkauf", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-einkauf-lead", "ou-einkauf", "Head of Procurement", "u-gf-1", List.of(), List.of("u-einkauf-asst"));
        for (int i = 1; i <= 3; i++) {
            person("u-einkauf-" + i, "ou-einkauf", "Einkäufer:in", "u-einkauf-lead", List.of(), List.of(), i > 1);
        }
        // u-einkauf-asst is APPENDED in buildSalesAndHr() (at the end), so that the deterministic
        // name assignment of the existing 88 persons remains unchanged (reference parity).

        unit("ou-legal", "Rechtsabteilung", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-legal-lead", "ou-legal", "Syndikus", "u-gf-1", List.of(), List.of());
        for (int i = 1; i <= 2; i++) {
            person("u-legal-" + i, "ou-legal", "Legal Assistant", "u-legal-lead", List.of(), List.of(), i > 1);
        }

        unit("ou-compliance", "Compliance", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-compliance-lead", "ou-compliance", "Compliance-Leitung", "u-gf-1", List.of(), List.of());
        for (int i = 1; i <= 3; i++) {
            person("u-compliance-" + i, "ou-compliance", "Compliance-Referent:in", "u-compliance-lead", List.of(), List.of(), i > 1);
        }

        unit("ou-finance", "Finance", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-finance-cfo", "ou-finance", "CFO (Prokurist)", "u-gf-1", List.of(), List.of());
        for (int i = 1; i <= 4; i++) {
            person("u-finance-" + i, "ou-finance", "Finance-Referent:in", "u-finance-cfo", List.of(), List.of(), i > 1);
        }

        unit("ou-controlling", "Controlling", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-controlling-lead", "ou-controlling", "Controlling-Leitung", "u-gf-2", List.of(), List.of());
        for (int i = 1; i <= 4; i++) {
            person("u-controlling-" + i, "ou-controlling", "Controller:in", "u-controlling-lead", List.of(), List.of(), i > 1);
        }
    }

    /**
     * Sales + HR + procurement assistant: not present in the demo catalog, added for "ACME offline"
     * (ACME = org SoR, ADR-0003). Deliberately appended AT THE END — so the first 88 persons keep
     * their deterministic names/emails (bit parity with the reference company catalog is preserved).
     */
    private void buildSalesAndHr() {
        // Assistant for procurement (unit already exists, lead references it as an assistantKey).
        person("u-einkauf-asst", "ou-einkauf", "Assistenz Einkauf", "u-einkauf-lead", List.of(), List.of());

        unit("ou-vertrieb", "Vertrieb", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-vertrieb-lead", "ou-vertrieb", "Head of Sales", "u-gf-2", List.of(), List.of("u-vertrieb-asst"));
        for (int i = 1; i <= 4; i++) {
            person("u-vertrieb-" + i, "ou-vertrieb", "Vertriebsmitarbeiter:in", "u-vertrieb-lead", List.of(), List.of(), i > 1);
        }
        person("u-vertrieb-asst", "ou-vertrieb", "Assistenz Vertrieb", "u-vertrieb-lead", List.of(), List.of());

        unit("ou-hr", "HR", OrgUnitType.DEPARTMENT, "ou-gf");
        person("u-hr-lead", "ou-hr", "HR-Leitung", "u-gf-1", List.of(), List.of("u-hr-asst"));
        for (int i = 1; i <= 2; i++) {
            person("u-hr-" + i, "ou-hr", "HR-Referent:in", "u-hr-lead", List.of(), List.of(), i > 1);
        }
        person("u-hr-asst", "ou-hr", "Assistenz HR", "u-hr-lead", List.of(), List.of());
    }

    private void unit(String key, String name, OrgUnitType type, String parentKey) {
        units.add(new UnitSpec(key, name, type, parentKey));
    }

    private void person(String key, String unitKey, String title, String managerKey,
                        List<String> delegateKeys, List<String> assistantKeys) {
        person(key, unitKey, title, managerKey, delegateKeys, assistantKeys, false);
    }

    private void person(String key, String unitKey, String title, String managerKey,
                        List<String> delegateKeys, List<String> assistantKeys, boolean applicant) {
        String displayName = NAMES.get(key);
        if (displayName == null) { // Fallback: generic name (should not apply for any of the 99 keys)
            String first = FIRST[nameSeq % FIRST.length];
            String last = LAST[(nameSeq / FIRST.length + nameSeq * 3) % LAST.length];
            nameSeq++;
            displayName = first + " " + last;
        }
        int sp = displayName.indexOf(' ');
        String first = sp < 0 ? displayName : displayName.substring(0, sp);
        String last = sp < 0 ? "" : displayName.substring(sp + 1);
        persons.add(new PersonSpec(key, displayName, email(first, last), unitKey, title,
                managerKey, List.copyOf(delegateKeys), List.copyOf(assistantKeys), applicant));
    }

    private String email(String first, String last) {
        String local = ascii(first).charAt(0) + "." + ascii(last);
        String candidate = local + "@acme-group.io";
        int n = 2;
        while (!usedEmails.add(candidate)) {
            candidate = local + (n++) + "@acme-group.io";
        }
        return candidate;
    }

    private static String ascii(String s) {
        String x = s.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return x.replaceAll("[^a-z0-9]", "");
    }
}
