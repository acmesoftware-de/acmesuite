-- ACMEcrm: quotes + orders (with e-approval fields) and their line items.

create table quote (
    id          varchar(48) primary key,
    customer_id varchar(48) not null references customer (id),
    status      varchar(16) not null,
    currency    varchar(3)  not null,
    valid_until date,
    created_on  date
);
create table quote_line (
    quote_id         varchar(48) not null references quote (id),
    product_id       varchar(48) not null,
    quantity         integer not null,
    unit_price       numeric(18, 2) not null,
    discount_percent numeric(6, 2)
);
create index idx_quote_line on quote_line (quote_id);

create table sales_order (
    id                  varchar(48) primary key,
    customer_id         varchar(48) not null references customer (id),
    quote_id            varchar(48),
    status              varchar(20) not null,
    order_date          date not null,
    total_amount        numeric(18, 2),
    total_currency      varchar(3),
    note                varchar(512),
    approval_required   boolean not null default false,
    approver_id         varchar(48),
    approval_decision   varchar(16),
    approval_decided_on date,
    approval_comment    varchar(512)
);
create index idx_order_customer on sales_order (customer_id);
create index idx_order_status on sales_order (status);

create table order_line (
    order_id         varchar(48) not null references sales_order (id),
    product_id       varchar(48) not null,
    quantity         integer not null,
    unit_price       numeric(18, 2) not null,
    discount_percent numeric(6, 2)
);
create index idx_order_line on order_line (order_id);
