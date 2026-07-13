-- ACMEassist audit trail (ADR-0008 governance G2): one append-only, hash-chained row per turn.
-- NB: number to reconcile at integration — the base-search branch also introduces a V23.
create table assist_audit (
    id              bigserial primary key,
    conversation_id varchar(64),
    user_id         varchar(64)  not null,
    user_role       varchar(16),
    agent           varchar(64),
    provider        varchar(32),
    model           varchar(96),
    tools           text,
    outcome         varchar(16)  not null,
    prompt_version  varchar(32),
    created_at      timestamptz  not null default now(),
    prev_hash       varchar(64),
    hash            varchar(64)  not null
);

create index idx_assist_audit_user on assist_audit (user_id);
create index idx_assist_audit_created on assist_audit (created_at);
