-- ACMEprod: bills of materials (BOM) per product — raw materials + labor/energy units (kWh) per unit.

create table product_bom (
    product_id   varchar(48) primary key,
    labor_units  numeric(12, 2) not null,
    energy_units numeric(12, 2) not null
);

create table product_bom_line (
    product_id  varchar(48) not null references product_bom (product_id),
    material_id varchar(48) not null,
    quantity    numeric(12, 3) not null
);
create index idx_bom_line on product_bom_line (product_id);
