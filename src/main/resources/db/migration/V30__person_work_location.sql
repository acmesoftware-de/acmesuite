-- ACMEhr Team roster: employees have a primary work location (on-site / remote / hybrid).
-- Person is @Audited (ADR-0010) → mirror the column into the Envers person_aud table.
alter table person
    add column work_location varchar(16) not null default 'ONSITE';

alter table person_aud
    add column work_location varchar(16);
