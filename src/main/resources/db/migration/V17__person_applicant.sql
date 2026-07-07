-- Smaller startup company: most team members start as applicants and are hired
-- via an approval contract; 30-day block after a rejection.
alter table person
    add column applicant              boolean not null default false,
    add column hire_blocked_until_day bigint  not null default -1;
