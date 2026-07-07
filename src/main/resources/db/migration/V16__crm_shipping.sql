-- Shipping mode per order (ACMEcrm). The shipping tuning knobs (configuration parameters) live elsewhere.
alter table sales_order add column shipping_mode varchar(8) not null default 'SHIP';
