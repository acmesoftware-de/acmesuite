-- Partial fulfillment: quantity already produced per order line. Allows large orders to be worked off
-- over several days (capacity as a real throttle instead of an atomic all-or-nothing).
-- OrderLine is a shared @Embeddable of order (order_line) AND quote (quote_line) —
-- for schema validation the column must exist in both tables (unused for quotes).
ALTER TABLE order_line ADD COLUMN fulfilled_quantity INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quote_line ADD COLUMN fulfilled_quantity INTEGER NOT NULL DEFAULT 0;
