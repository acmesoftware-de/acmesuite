-- ACMEcrm: contacts (people at a customer), the sales pipeline overlay (deals) and mail
-- threads (correspondence). All three entities are AuditedEntity (ADR-0010: audit + tombstone
-- columns, Envers _aud history). Mail participants/messages are @ElementCollection (@NotAudited).

-- ── Contacts ───────────────────────────────────────────────
create table contact (
    id          varchar(48) primary key,
    customer_id varchar(48) references customer (id),
    name        varchar(255) not null,
    role        varchar(120),
    email       varchar(160),
    phone       varchar(48),
    is_primary  boolean not null default false,
    newsletter  boolean not null default false,
    created_at  timestamp with time zone not null,
    created_by  varchar(64),
    updated_at  timestamp with time zone not null,
    updated_by  varchar(64),
    deleted_at  timestamp with time zone,
    deleted_by  varchar(64)
);
create index idx_contact_customer on contact (customer_id);

create table contact_aud (
    rev         integer not null,
    revtype     smallint,
    id          varchar(48) not null,
    customer_id varchar(48),
    name        varchar(255),
    role        varchar(120),
    email       varchar(160),
    phone       varchar(48),
    is_primary  boolean,
    newsletter  boolean,
    created_at  timestamp with time zone,
    created_by  varchar(64),
    updated_at  timestamp with time zone,
    updated_by  varchar(64),
    deleted_at  timestamp with time zone,
    deleted_by  varchar(64),
    primary key (rev, id),
    constraint fk_contact_aud_rev foreign key (rev) references revinfo (rev)
);

-- ── Pipeline (deal overlay) ────────────────────────────────
create table deal (
    id               varchar(48) primary key,
    source           varchar(16) not null,
    customer_id      varchar(48),
    contact_id       varchar(48),
    company          varchar(255) not null,
    contact          varchar(200),
    stage            varchar(16) not null,
    owner_initials   varchar(3),
    owner_name       varchar(120),
    value_amount     numeric(18, 2),
    value_currency   varchar(3),
    quote_id         varchar(48),
    order_id         varchar(48),
    last_activity    varchar(200),
    last_activity_at timestamp with time zone,
    stage_since      date not null,
    created_at       timestamp with time zone not null,
    created_by       varchar(64),
    updated_at       timestamp with time zone not null,
    updated_by       varchar(64),
    deleted_at       timestamp with time zone,
    deleted_by       varchar(64)
);
create index idx_deal_customer on deal (customer_id);
create index idx_deal_contact on deal (contact_id);
create index idx_deal_stage on deal (stage);

create table deal_aud (
    rev              integer not null,
    revtype          smallint,
    id               varchar(48) not null,
    source           varchar(16),
    customer_id      varchar(48),
    contact_id       varchar(48),
    company          varchar(255),
    contact          varchar(200),
    stage            varchar(16),
    owner_initials   varchar(3),
    owner_name       varchar(120),
    value_amount     numeric(18, 2),
    value_currency   varchar(3),
    quote_id         varchar(48),
    order_id         varchar(48),
    last_activity    varchar(200),
    last_activity_at timestamp with time zone,
    stage_since      date,
    created_at       timestamp with time zone,
    created_by       varchar(64),
    updated_at       timestamp with time zone,
    updated_by       varchar(64),
    deleted_at       timestamp with time zone,
    deleted_by       varchar(64),
    primary key (rev, id),
    constraint fk_deal_aud_rev foreign key (rev) references revinfo (rev)
);

-- ── Mail threads (read-only overlay) ───────────────────────
create table mail_thread (
    id              varchar(48) primary key,
    customer_id     varchar(48),
    contact_id      varchar(48),
    subject         varchar(255) not null,
    last_message_at timestamp with time zone,
    created_at      timestamp with time zone not null,
    created_by      varchar(64),
    updated_at      timestamp with time zone not null,
    updated_by      varchar(64),
    deleted_at      timestamp with time zone,
    deleted_by      varchar(64)
);
create index idx_mail_thread_customer on mail_thread (customer_id);
create index idx_mail_thread_contact on mail_thread (contact_id);

create table mail_thread_aud (
    rev             integer not null,
    revtype         smallint,
    id              varchar(48) not null,
    customer_id     varchar(48),
    contact_id      varchar(48),
    subject         varchar(255),
    last_message_at timestamp with time zone,
    created_at      timestamp with time zone,
    created_by      varchar(64),
    updated_at      timestamp with time zone,
    updated_by      varchar(64),
    deleted_at      timestamp with time zone,
    deleted_by      varchar(64),
    primary key (rev, id),
    constraint fk_mail_thread_aud_rev foreign key (rev) references revinfo (rev)
);

create table mail_thread_participant (
    thread_id   varchar(48) not null references mail_thread (id),
    participant varchar(160)
);
create index idx_mail_participant_thread on mail_thread_participant (thread_id);

create table mail_message (
    thread_id  varchar(48) not null references mail_thread (id),
    position   integer not null,
    message_id varchar(48),
    direction  varchar(16) not null,
    from_addr  varchar(160),
    to_addr    varchar(160),
    sent_at    timestamp with time zone not null,
    snippet    varchar(240),
    body       varchar(4000),
    primary key (thread_id, position)
);
