package de.acmesoftware.acmesuite.base;

import org.hibernate.envers.RevisionListener;

/** Stamps the acting user onto each Envers revision. */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setActor(CurrentActor.current());
    }
}
