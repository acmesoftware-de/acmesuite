-- ACMEbase auth: local user directory with locally-assigned roles (WATCH/WORK/ADMIN).
-- Identity may be federated (Entra/OIDC) or local; authorization (role) is always local.
-- Deliberately separate from org.person (business employees): this is API access only.
create table base_user (
    id                varchar(32)  primary key,
    -- Local login name (null for purely federated users).
    username          varchar(160) unique,
    email             varchar(200),
    display_name      varchar(200),
    -- Access role on the suite APIs: WATCH < WORK < ADMIN.
    role              varchar(16)  not null,
    -- ACTIVE (may sign in), PENDING (federated, awaiting role assignment), DISABLED.
    status            varchar(16)  not null,
    -- Which provider authenticates this user: 'local' or a configured provider id.
    auth_provider     varchar(64)  not null,
    -- Stable external subject (oid/sub) for federated users; null for local.
    external_subject  varchar(200),
    -- BCrypt hash for local users; null for federated.
    password_hash     varchar(200),
    -- Forces a password change on next login (bootstrap admin / admin reset).
    must_set_password boolean      not null default false,
    created_at        timestamptz  not null,
    updated_at        timestamptz  not null,
    -- A federated identity maps to exactly one local user per provider.
    constraint uq_base_user_provider_subject unique (auth_provider, external_subject)
);

create index ix_base_user_email on base_user (email);
