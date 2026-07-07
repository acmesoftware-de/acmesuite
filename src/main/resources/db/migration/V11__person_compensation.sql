-- Compensation per person (payroll). Wage/hour vs. salary + hourly rate. Set defaults directly in the
-- existing DB (the seeder does not run again on an already-seeded DB): managing directors (without a
-- manager) and leadership (-lead/-cfo) = salary (50 / 35 €/h basis), everyone else = hourly wage 25 €/h.
ALTER TABLE person ADD COLUMN comp_type   VARCHAR(16)    NOT NULL DEFAULT 'HOURLY';
ALTER TABLE person ADD COLUMN hourly_rate NUMERIC(10, 2) NOT NULL DEFAULT 25.00;

UPDATE person SET comp_type = 'SALARIED', hourly_rate = 35.00
    WHERE id LIKE '%-lead' OR id LIKE '%-cfo';
UPDATE person SET comp_type = 'SALARIED', hourly_rate = 50.00
    WHERE manager_id IS NULL;
