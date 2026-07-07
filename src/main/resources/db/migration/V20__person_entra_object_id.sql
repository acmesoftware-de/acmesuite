-- Entra object id (oid) per person: set by the Entra provisioner after the Graph upsert (HR -> Entra,
-- ACMEhr is the system of record). Anchor for the later SSO subject matching (token sub = Entra oid).
-- Null as long as the person is not provisioned into Entra.
alter table person
    add column entra_object_id varchar(64);
