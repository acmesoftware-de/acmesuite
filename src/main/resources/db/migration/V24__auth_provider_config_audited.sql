-- ADR-0010: versioned + tombstoned data. Retrofit auth_provider_config as the reference:
-- audit/tombstone columns, uniqueness scoped to live rows, and the Envers revision history.

-- created_at / updated_at / updated_by already exist (V22); add the rest.
alter table auth_provider_config
    add column created_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);

-- A provider id is unique only among live rows (reusable after its config is tombstoned).
alter table auth_provider_config drop constraint auth_provider_config_provider_id_key;
create unique index uq_auth_provider_config_provider_id_live
    on auth_provider_config (provider_id) where deleted_at is null;

-- Envers revision registry (who/when behind every version).
create sequence revinfo_seq start with 1 increment by 1;
create table revinfo (
    rev      integer not null,
    revtstmp bigint,
    actor    varchar(64),
    primary key (rev)
);

-- Envers history table for auth_provider_config.
create table auth_provider_config_aud (
    id           varchar(32) not null,
    rev          integer     not null,
    revtype      smallint,
    provider_id  varchar(64),
    display_name varchar(160),
    enabled      boolean,
    config_json  text,
    secrets_json text,
    created_at   timestamp with time zone,
    created_by   varchar(64),
    updated_at   timestamp with time zone,
    updated_by   varchar(64),
    deleted_at   timestamp with time zone,
    deleted_by   varchar(64),
    primary key (id, rev),
    constraint fk_apc_aud_rev foreign key (rev) references revinfo (rev)
);
