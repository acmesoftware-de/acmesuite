-- ADR-0010: versioned + tombstoned data. Retrofit base_user like auth_provider_config (V24):
-- audit/tombstone columns, uniqueness scoped to live rows, the Envers history table, and the
-- orthogonal AUDIT capability (may view version history).

-- created_at / updated_at already exist (V21); add the rest plus the AUDIT flag.
alter table base_user
    add column created_by varchar(64),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64),
    add column auditor    boolean not null default false;

-- Username and (provider, subject) are unique only among live rows (reusable after a tombstone).
alter table base_user drop constraint base_user_username_key;
alter table base_user drop constraint uq_base_user_provider_subject;
create unique index uq_base_user_username_live
    on base_user (username) where deleted_at is null;
create unique index uq_base_user_provider_subject_live
    on base_user (auth_provider, external_subject) where deleted_at is null;

-- Envers history table for base_user (revinfo/revinfo_seq created in V24).
create table base_user_aud (
    id                varchar(32) not null,
    rev               integer     not null,
    revtype           smallint,
    username          varchar(160),
    email             varchar(200),
    display_name      varchar(200),
    role              varchar(16),
    status            varchar(16),
    auth_provider     varchar(64),
    external_subject  varchar(200),
    password_hash     varchar(200),
    must_set_password boolean,
    auditor           boolean,
    created_at        timestamp with time zone,
    created_by        varchar(64),
    updated_at        timestamp with time zone,
    updated_by        varchar(64),
    deleted_at        timestamp with time zone,
    deleted_by        varchar(64),
    primary key (id, rev),
    constraint fk_base_user_aud_rev foreign key (rev) references revinfo (rev)
);
