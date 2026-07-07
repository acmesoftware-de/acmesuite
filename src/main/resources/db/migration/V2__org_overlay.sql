-- Curated overlay of the canonical org catalog (manager/delegate/assistant/secondary) +
-- time-bounded absences with substitution. Filled by AcmeOrgCatalog/OrgSeeder at runtime.

alter table person add column active boolean not null default true;
alter table person add column manager_id varchar(32) references person (id);

create table person_delegate (
    person_id   varchar(32) not null references person (id),
    delegate_id varchar(32) not null,
    primary key (person_id, delegate_id)
);

create table person_assistant (
    person_id    varchar(32) not null references person (id),
    assistant_id varchar(32) not null,
    primary key (person_id, assistant_id)
);

create table person_secondary_unit (
    person_id varchar(32) not null references person (id),
    unit_id   varchar(96) not null,
    primary key (person_id, unit_id)
);

create table absence (
    id            varchar(48) primary key,
    person_id     varchar(32) not null references person (id),
    reason_key    varchar(64) not null,
    substitute_id varchar(32) references person (id),
    valid_from    date,
    valid_until   date
);
