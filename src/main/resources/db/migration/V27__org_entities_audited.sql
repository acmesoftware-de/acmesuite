-- ADR-0010: versioned + tombstoned data across the org module (incl. HR: person, absence,
-- approval_limit, plus the org backbone legal_entity/org_unit/cost_center/role/
-- role_assignment/power_of_attorney). Each entity extends AuditedEntity. Unlike crm/supply/
-- build these use @ManyToOne associations, so the whole graph is audited together (the FK id
-- is captured in each _AUD row). @ElementCollection fields on person are @NotAudited.
-- Schema derived from Hibernate's own generation; revinfo/revinfo_seq exist (V24).

-- ── Audit + tombstone columns on the 9 org entity tables ─────────────────
alter table legal_entity
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table legal_entity alter column created_at drop default, alter column updated_at drop default;

alter table org_unit
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table org_unit alter column created_at drop default, alter column updated_at drop default;

alter table person
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table person alter column created_at drop default, alter column updated_at drop default;

alter table role
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table role alter column created_at drop default, alter column updated_at drop default;

alter table role_assignment
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table role_assignment alter column created_at drop default, alter column updated_at drop default;

alter table cost_center
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table cost_center alter column created_at drop default, alter column updated_at drop default;

alter table power_of_attorney
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table power_of_attorney alter column created_at drop default, alter column updated_at drop default;

alter table approval_limit
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table approval_limit alter column created_at drop default, alter column updated_at drop default;

alter table absence
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table absence alter column created_at drop default, alter column updated_at drop default;

-- ── Envers history tables (entity _AUD; @NotAudited collections excluded) ─
create table legal_entity_aud (
    country varchar(2),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    type varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    id varchar(64) not null,
    parent_id varchar(64),
    platform_tenant_key varchar(64),
    registration_number varchar(64),
    updated_by varchar(64),
    legal_name varchar(255),
    primary key (rev, id),
    constraint fk_legal_entity_aud_rev foreign key (rev) references revinfo (rev)
);

create table org_unit_aud (
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    type varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    legal_entity_id varchar(64),
    updated_by varchar(64),
    id varchar(96) not null,
    parent_id varchar(96),
    name varchar(255),
    primary key (rev, id),
    constraint fk_org_unit_aud_rev foreign key (rev) references revinfo (rev)
);

create table person_aud (
    active boolean,
    applicant boolean,
    hourly_rate numeric(10,2),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    hire_blocked_until_day bigint,
    updated_at timestamp with time zone,
    comp_type varchar(16),
    id varchar(32) not null,
    manager_id varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    entra_object_id varchar(64),
    updated_by varchar(64),
    primary_org_unit_id varchar(96),
    email varchar(160),
    first_name varchar(255),
    job_title varchar(255),
    last_name varchar(255),
    primary key (rev, id),
    constraint fk_person_aud_rev foreign key (rev) references revinfo (rev)
);

create table role_aud (
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    kind varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    id varchar(64) not null,
    updated_by varchar(64),
    description varchar(512),
    title varchar(255),
    primary key (rev, id),
    constraint fk_role_aud_rev foreign key (rev) references revinfo (rev)
);

create table role_assignment_aud (
    rev integer not null,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    person_id varchar(32),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    role_id varchar(64),
    updated_by varchar(64),
    org_unit_id varchar(96),
    primary key (rev, id),
    constraint fk_role_assignment_aud_rev foreign key (rev) references revinfo (rev)
);

create table cost_center_aud (
    budget_amount numeric(38,2),
    budget_currency varchar(3),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    id varchar(32) not null,
    responsible_person_id varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    org_unit_id varchar(96),
    name varchar(255),
    primary key (rev, id),
    constraint fk_cost_center_aud_rev foreign key (rev) references revinfo (rev)
);

create table power_of_attorney_aud (
    limit_amount numeric(38,2),
    limit_currency varchar(3),
    rev integer not null,
    revoked boolean,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    signature_rule varchar(16),
    person_id varchar(32),
    type varchar(32),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    legal_entity_id varchar(64),
    updated_by varchar(64),
    scope varchar(512),
    primary key (rev, id),
    constraint fk_power_of_attorney_aud_rev foreign key (rev) references revinfo (rev)
);

create table approval_limit_aud (
    max_amount numeric(38,2),
    max_currency varchar(3),
    rev integer not null,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    person_id varchar(32),
    created_by varchar(64),
    deleted_by varchar(64),
    id varchar(64) not null,
    legal_entity_id varchar(64),
    updated_by varchar(64),
    primary key (rev, id),
    constraint fk_approval_limit_aud_rev foreign key (rev) references revinfo (rev)
);

create table absence_aud (
    rev integer not null,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    status varchar(16),
    type varchar(16),
    person_id varchar(32),
    substitute_id varchar(32),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    reason_key varchar(64),
    updated_by varchar(64),
    note varchar(512),
    primary key (rev, id),
    constraint fk_absence_aud_rev foreign key (rev) references revinfo (rev)
);
