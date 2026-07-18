-- ACMEhr recruiting: applicants carry a pipeline stage, a fit score and an application date
-- (design module 02 "Bewerber"). Person is @Audited (ADR-0010), so mirror the columns into
-- the Envers person_aud table as well.
alter table person
    add column applicant_stage varchar(16),
    add column match_score     integer,
    add column applied_on      date;

alter table person_aud
    add column applicant_stage varchar(16),
    add column match_score     integer,
    add column applied_on      date;

-- Backfill: existing applicants enter the pipeline at NEW; hired employees stay null.
update person set applicant_stage = 'NEW' where applicant = true and applicant_stage is null;
