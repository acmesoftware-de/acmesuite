package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.web.AdminViews.HistoryEntry;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the full version history of a Base user from the Envers audit tables (ADR-0010). Exposed
 * only to the AUDIT capability — ordinary users never see version numbers or earlier versions.
 */
@Service
public class UserHistoryService {

    private final EntityManager em;

    public UserHistoryService(EntityManager em) {
        this.em = em;
    }

    /** Every revision of the given user, oldest first; empty if the id is unknown. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<HistoryEntry> history(String userId) {
        AuditReader reader = AuditReaderFactory.get(em);
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(BaseUser.class, false, true)
                .add(AuditEntity.id().eq(userId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        List<HistoryEntry> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            BaseUser u = (BaseUser) row[0];
            AuditRevision rev = (AuditRevision) row[1];
            RevisionType type = (RevisionType) row[2];
            out.add(new HistoryEntry(
                    rev.getId(),
                    Instant.ofEpochMilli(rev.getTimestamp()).toString(),
                    rev.getActor(),
                    type.name(),
                    u.getUsername(), u.getEmail(), u.getDisplayName(),
                    u.getRole().name(), u.getStatus().name(),
                    u.isAuditor(), u.isDeleted()));
        }
        return out;
    }
}
