-- ACMEsupply: suppliers, material (raw material/energy), supply contracts (tiers+lead time), procurements.

create table supplier (
    id      varchar(48) primary key,
    name    varchar(255) not null,
    status  varchar(16)  not null,
    email   varchar(160),
    country varchar(3)
);

create table material (
    id   varchar(48) primary key,
    code varchar(48)  not null,
    name varchar(255) not null,
    kind varchar(16)  not null,
    unit varchar(16)
);

create table supply_contract (
    id             varchar(48) primary key,
    supplier_id    varchar(48) not null references supplier (id),
    material_id    varchar(48) not null references material (id),
    currency       varchar(3)  not null,
    lead_time_days integer     not null,
    valid_from     date,
    valid_until    date
);
create index idx_sc_supplier on supply_contract (supplier_id);

create table supply_contract_tier (
    contract_id  varchar(48) not null references supply_contract (id),
    min_quantity integer not null default 1,
    unit_price   numeric(18, 2) not null
);
create index idx_sct_contract on supply_contract_tier (contract_id);

create table supply_order (
    id                    varchar(48) primary key,
    supplier_id           varchar(48) not null references supplier (id),
    status                varchar(20) not null,
    order_date            date not null,
    expected_delivery_date date,
    total_amount          numeric(18, 2),
    total_currency        varchar(3),
    note                  varchar(512),
    approval_required     boolean not null default false,
    approver_id           varchar(48),
    approval_decision     varchar(16),
    approval_decided_on   date,
    approval_comment      varchar(512)
);
create index idx_so_supplier on supply_order (supplier_id);
create index idx_so_status on supply_order (status);

create table supply_order_line (
    order_id    varchar(48) not null references supply_order (id),
    material_id varchar(48) not null,
    quantity    integer not null,
    unit_price  numeric(18, 2) not null
);
create index idx_sol_order on supply_order_line (order_id);
