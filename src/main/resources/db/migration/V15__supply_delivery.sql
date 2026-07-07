-- Slice 4: delivery mode per procurement (ship/plane).
alter table supply_order add column delivery_mode varchar(8) not null default 'SHIP';
