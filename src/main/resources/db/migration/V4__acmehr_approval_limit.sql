-- ACMEhr: explicitly maintained approval limits (override the limit derived from powers of attorney).

create table approval_limit (
    id              varchar(64) primary key,
    person_id       varchar(32) not null references person (id),
    legal_entity_id varchar(64) references legal_entity (id),
    max_amount      numeric(18, 2),
    max_currency    varchar(3),
    valid_from      date,
    valid_until     date
);

create index idx_approval_limit_person on approval_limit (person_id);
