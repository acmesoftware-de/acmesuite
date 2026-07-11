-- Configuration for federated auth providers (Entra, generic OIDC), managed by an admin.
-- Non-secret fields live in config_json; secret fields (client secrets) live in secrets_json
-- envelope-encrypted (AES-256-GCM) and are never returned in clear.
create table auth_provider_config (
    id            varchar(32)  primary key,
    -- Matches an AuthProvider bean id ("entra", "oidc"). One config per provider.
    provider_id   varchar(64)  not null unique,
    display_name  varchar(160),
    enabled       boolean      not null default false,
    config_json   text,
    secrets_json  text,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    updated_by    varchar(64)
);
