-- Company model of the ACME Group (schema ownership: Flyway, Hibernate only validates).
-- Natural string keys for readable seeds and browsable URLs.

create table legal_entity (
    id                  varchar(64)  primary key,
    legal_name          varchar(255) not null,
    type                varchar(32)  not null,
    parent_id           varchar(64)  references legal_entity (id),
    country             varchar(2),
    registration_number varchar(64),
    platform_tenant_key    varchar(64)
);

create table org_unit (
    id              varchar(96)  primary key,
    name            varchar(255) not null,
    type            varchar(32)  not null,
    legal_entity_id varchar(64)  not null references legal_entity (id),
    parent_id       varchar(96)  references org_unit (id)
);

create index idx_org_unit_legal_entity on org_unit (legal_entity_id);

create table person (
    id                  varchar(32)  primary key,
    first_name          varchar(255) not null,
    last_name           varchar(255) not null,
    email               varchar(160),
    job_title           varchar(255),
    primary_org_unit_id varchar(96)  references org_unit (id)
);

create table role (
    id          varchar(64)  primary key,
    title       varchar(255) not null,
    kind        varchar(32)  not null,
    description varchar(512)
);

create table role_assignment (
    id          varchar(48) primary key,
    person_id   varchar(32) not null references person (id),
    role_id     varchar(64) not null references role (id),
    org_unit_id varchar(96) references org_unit (id),
    valid_from  date,
    valid_until date
);

create index idx_role_assignment_person on role_assignment (person_id);

create table cost_center (
    id                    varchar(32)  primary key,
    name                  varchar(255) not null,
    org_unit_id           varchar(96)  not null references org_unit (id),
    responsible_person_id varchar(32)  references person (id),
    budget_amount         numeric(19, 2),
    budget_currency       varchar(3)
);

create table power_of_attorney (
    id              varchar(48)  primary key,
    person_id       varchar(32)  not null references person (id),
    legal_entity_id varchar(64)  not null references legal_entity (id),
    type            varchar(32)  not null,
    signature_rule  varchar(16)  not null,
    limit_amount    numeric(19, 2),
    limit_currency  varchar(3),
    scope           varchar(512),
    valid_from      date,
    valid_until     date,
    revoked         boolean      not null default false
);

create index idx_poa_legal_entity on power_of_attorney (legal_entity_id);
create index idx_poa_person on power_of_attorney (person_id);
