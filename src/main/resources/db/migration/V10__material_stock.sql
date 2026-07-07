-- ACMEsupply warehouse: stock per raw material. Production (daily cycle) draws from it,
-- goods receipt of received procurement replenishes it. Energy is not stocked.
CREATE TABLE material_stock (
    material_id   VARCHAR(48)    PRIMARY KEY,
    quantity      NUMERIC(16, 3) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(16, 3) NOT NULL DEFAULT 0
);
