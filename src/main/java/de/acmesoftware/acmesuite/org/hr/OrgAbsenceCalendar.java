package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.org.domain.Absence;
import de.acmesoftware.acmesuite.org.domain.AbsenceRepository;
import de.acmesoftware.acmesuite.org.domain.AbsenceStatus;
import de.acmesoftware.acmesuite.shared.AbsenceCalendar;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ACMEhr adapter of the {@link AbsenceCalendar}: maps the day number to a calendar date
 * (configured start day) and returns the absences in effect on that day (not
 * cancelled/rejected). This way, vacation/sick leave run off real HR data.
 */
@Component
public class OrgAbsenceCalendar implements AbsenceCalendar {

    private final AbsenceRepository absences;
    private final LocalDate startDate;

    public OrgAbsenceCalendar(AbsenceRepository absences,
                              @Value("${acme.sim.start-date:2026-01-01}") LocalDate startDate) {
        this.absences = absences;
        this.startDate = startDate;
    }

    @Override
    public LocalDate dateOf(long simDay) {
        return startDate.plusDays(simDay);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> absencesOnDay(long simDay) {
        LocalDate date = dateOf(simDay);
        Map<String, String> out = new LinkedHashMap<>();
        for (Absence a : absences.findAll()) {
            if (a.getStatus() == AbsenceStatus.CANCELLED || a.getStatus() == AbsenceStatus.REJECTED) {
                continue;
            }
            if (a.coversDate(date)) {
                out.put(a.getPerson().getId(), a.getType().name());
            }
        }
        return out;
    }
}
