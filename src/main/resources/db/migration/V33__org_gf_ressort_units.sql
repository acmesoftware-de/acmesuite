-- The two managing directors each lead a portfolio, and the unit name carries its short code
-- (CFO, CEO). A downstream platform reads the portfolio as the primary unit name of the person at
-- the top of a reporting line, so each director needs a unit of her own below "Geschäftsführung".
--
-- Data only, for databases seeded before this change. On a fresh database Flyway runs before
-- OrgSeeder, ou-gf does not exist yet and every statement below touches no row; the seeder then
-- builds the same structure from AcmeOrgCatalog. Like V29, no Envers rows are written.

insert into org_unit (id, name, type, legal_entity_id, parent_id, created_at, updated_at)
select 'ou-cfo', 'CFO', 'DEPARTMENT', gf.legal_entity_id, gf.id, now(), now()
from org_unit gf
where gf.id = 'ou-gf'
  and not exists (select 1 from org_unit where id = 'ou-cfo');

insert into org_unit (id, name, type, legal_entity_id, parent_id, created_at, updated_at)
select 'ou-ceo', 'CEO', 'DEPARTMENT', gf.legal_entity_id, gf.id, now(), now()
from org_unit gf
where gf.id = 'ou-gf'
  and not exists (select 1 from org_unit where id = 'ou-ceo');

-- Move only a director who still sits in ou-gf: a unit changed by hand since stays as it is.
update person set primary_org_unit_id = 'ou-cfo', updated_at = now()
where id = 'u-gf-1' and primary_org_unit_id = 'ou-gf'
  and exists (select 1 from org_unit where id = 'ou-cfo');

update person set primary_org_unit_id = 'ou-ceo', updated_at = now()
where id = 'u-gf-2' and primary_org_unit_id = 'ou-gf'
  and exists (select 1 from org_unit where id = 'ou-ceo');
