-- ACMEcrm: customers/resellers, product catalog, price lists (list/reseller) with tiers.

create table customer (
    id                 varchar(48) primary key,
    name               varchar(255) not null,
    kind               varchar(16)  not null,
    status             varchar(16)  not null,
    email              varchar(160),
    country            varchar(3),
    parent_reseller_id varchar(48),
    price_list_id      varchar(48)
);
create index idx_customer_kind on customer (kind);

create table product (
    id            varchar(48) primary key,
    sku           varchar(64)  not null unique,
    name          varchar(255) not null,
    category      varchar(64),
    unit          varchar(24),
    active        boolean      not null default true,
    list_amount   numeric(18, 2),
    list_currency varchar(3)
);

create table price_list (
    id         varchar(48) primary key,
    name       varchar(255) not null,
    currency   varchar(3)   not null,
    kind       varchar(16)  not null,
    valid_from date,
    valid_until date
);

create table price_list_item (
    price_list_id varchar(48) not null references price_list (id),
    product_id    varchar(48) not null,
    unit_price    numeric(18, 2) not null,
    min_quantity  integer not null default 1
);
create index idx_pli_list on price_list_item (price_list_id);
