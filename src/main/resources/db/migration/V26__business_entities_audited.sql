-- ADR-0010: versioned + tombstoned data across the business modules (crm/supply/build).
-- Retrofits the 11 entity tables (customer/product/price_list/quote/sales_order/supplier/
-- material/supply_contract/supply_order/material_stock/product_bom) onto AuditedEntity:
-- audit + tombstone columns, uniqueness scoped to live rows, and the Envers history tables
-- (entity _AUD only; @ElementCollection line items are @NotAudited). revinfo exists (V24).

-- ── Audit + tombstone columns on the 11 entity tables ─────────────────
alter table customer
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table customer alter column created_at drop default, alter column updated_at drop default;

alter table product
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table product alter column created_at drop default, alter column updated_at drop default;

alter table price_list
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table price_list alter column created_at drop default, alter column updated_at drop default;

alter table quote
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table quote alter column created_at drop default, alter column updated_at drop default;

alter table sales_order
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table sales_order alter column created_at drop default, alter column updated_at drop default;

alter table supplier
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table supplier alter column created_at drop default, alter column updated_at drop default;

alter table material
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table material alter column created_at drop default, alter column updated_at drop default;

alter table supply_contract
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table supply_contract alter column created_at drop default, alter column updated_at drop default;

alter table supply_order
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table supply_order alter column created_at drop default, alter column updated_at drop default;

alter table material_stock
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table material_stock alter column created_at drop default, alter column updated_at drop default;

alter table product_bom
    add column created_at timestamp with time zone not null default now(),
    add column created_by varchar(64),
    add column updated_at timestamp with time zone not null default now(),
    add column updated_by varchar(64),
    add column deleted_at timestamp with time zone,
    add column deleted_by varchar(64);
alter table product_bom alter column created_at drop default, alter column updated_at drop default;

-- ── Uniqueness scoped to live rows (reusable after a tombstone) ───────────
-- product.sku was UNIQUE (V5); make it unique only among non-tombstoned rows.
alter table product drop constraint product_sku_key;
create unique index uq_product_sku_live on product (sku) where deleted_at is null;

-- ── Envers history tables ───────────────────────────────
create table customer_aud (
    country varchar(3),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    kind varchar(16),
    status varchar(16),
    id varchar(48) not null,
    parent_reseller_id varchar(48),
    price_list_id varchar(48),
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    email varchar(160),
    name varchar(255),
    primary key (rev, id),
    constraint fk_customer_aud_rev foreign key (rev) references revinfo (rev)
);

create table material_aud (
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    kind varchar(16),
    unit varchar(16),
    code varchar(48),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    name varchar(255),
    primary key (rev, id),
    constraint fk_material_aud_rev foreign key (rev) references revinfo (rev)
);

create table material_stock_aud (
    quantity numeric(16,3),
    reorder_level numeric(16,3),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    material_id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    primary key (rev, material_id),
    constraint fk_material_stock_aud_rev foreign key (rev) references revinfo (rev)
);

create table price_list_aud (
    currency varchar(3),
    rev integer not null,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    kind varchar(16),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    name varchar(255),
    primary key (rev, id),
    constraint fk_price_list_aud_rev foreign key (rev) references revinfo (rev)
);

create table product_aud (
    active boolean,
    list_amount numeric(38,2),
    list_currency varchar(3),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    unit varchar(24),
    id varchar(48) not null,
    category varchar(64),
    created_by varchar(64),
    deleted_by varchar(64),
    sku varchar(64),
    updated_by varchar(64),
    name varchar(255),
    primary key (rev, id),
    constraint fk_product_aud_rev foreign key (rev) references revinfo (rev)
);

create table product_bom_aud (
    energy_units numeric(38,2),
    labor_units numeric(38,2),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    product_id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    primary key (rev, product_id),
    constraint fk_product_bom_aud_rev foreign key (rev) references revinfo (rev)
);

create table quote_aud (
    created_on date,
    currency varchar(3),
    rev integer not null,
    revtype smallint,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    status varchar(16),
    customer_id varchar(48),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    primary key (rev, id),
    constraint fk_quote_aud_rev foreign key (rev) references revinfo (rev)
);

create table sales_order_aud (
    approval_decided_on date,
    approval_required boolean,
    order_date date,
    rev integer not null,
    revtype smallint,
    total_amount numeric(38,2),
    total_currency varchar(3),
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    shipping_mode varchar(8),
    updated_at timestamp with time zone,
    approval_decision varchar(16),
    status varchar(20),
    approver_id varchar(48),
    customer_id varchar(48),
    id varchar(48) not null,
    quote_id varchar(48),
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    approval_comment varchar(512),
    note varchar(512),
    primary key (rev, id),
    constraint fk_sales_order_aud_rev foreign key (rev) references revinfo (rev)
);

create table supplier_aud (
    country varchar(3),
    rev integer not null,
    revtype smallint,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    status varchar(16),
    id varchar(48) not null,
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    email varchar(160),
    name varchar(255),
    primary key (rev, id),
    constraint fk_supplier_aud_rev foreign key (rev) references revinfo (rev)
);

create table supply_contract_aud (
    currency varchar(3),
    lead_time_days integer,
    rev integer not null,
    revtype smallint,
    valid_from date,
    valid_until date,
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    updated_at timestamp with time zone,
    id varchar(48) not null,
    material_id varchar(48),
    supplier_id varchar(48),
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    primary key (rev, id),
    constraint fk_supply_contract_aud_rev foreign key (rev) references revinfo (rev)
);

create table supply_order_aud (
    approval_decided_on date,
    approval_required boolean,
    expected_delivery_date date,
    order_date date,
    rev integer not null,
    revtype smallint,
    total_amount numeric(38,2),
    total_currency varchar(3),
    created_at timestamp with time zone,
    deleted_at timestamp with time zone,
    delivery_mode varchar(8),
    updated_at timestamp with time zone,
    approval_decision varchar(16),
    status varchar(20),
    approver_id varchar(48),
    id varchar(48) not null,
    supplier_id varchar(48),
    created_by varchar(64),
    deleted_by varchar(64),
    updated_by varchar(64),
    approval_comment varchar(512),
    note varchar(512),
    primary key (rev, id),
    constraint fk_supply_order_aud_rev foreign key (rev) references revinfo (rev)
);

