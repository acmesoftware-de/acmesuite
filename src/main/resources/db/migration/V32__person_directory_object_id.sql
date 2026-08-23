-- Rename the identity anchor to a vendor-neutral name (ADR-0011). The column was named after one
-- particular directory, while the value it holds is "this person's object id in whatever corporate
-- directory the installation uses" -- and the product is meant to speak to more than one. Only the
-- name changes: same type, same values, same meaning, and `subjectRef` keeps resolving against it.
-- person_aud is the Envers history of the same column and has to travel with it.
alter table person
    rename column entra_object_id to directory_object_id;

alter table person_aud
    rename column entra_object_id to directory_object_id;
