-- ACMEbuild shop floor (contract v0.2.0): production orders (planning board), the weekly
-- shift plan and the live machine monitor. Operational / telemetry data — deliberately NOT
-- Envers-audited (high churn, no change-history requirement), so no *_aud tables here.

-- Production orders on the planning board (GEPLANT · RUESTEN · IN_ARBEIT · PRUEFUNG · FERTIG).
create table production_order (
    id             varchar(48) primary key,
    order_no       varchar(24)  not null,
    product_id     varchar(48),
    product_name   varchar(120),
    quantity       integer      not null,
    machine        varchar(48),
    owner_initials varchar(8),
    stage          varchar(16)  not null,
    due_date       date
);
create index idx_production_order_stage on production_order (stage);

-- Weekly shift plan: one row per shift (EARLY | LATE | NIGHT); `cells` holds the six working
-- days Mon–Sat as a comma-joined list of FREE | FULL | PARTIAL.
create table shift_plan_row (
    shift      varchar(8)  primary key,
    ord        integer     not null,
    label      varchar(32) not null,
    time_range varchar(16) not null,
    cells      varchar(64) not null
);

-- Machine monitor (digital twin): live status + OEE and its components.
create table machine (
    id            varchar(48) primary key,
    name          varchar(48)  not null,
    status        varchar(16)  not null,
    oee           integer      not null,
    availability  integer      not null,
    performance   integer      not null,
    quality       integer      not null,
    progress      integer      not null,
    current_order varchar(120)
);
