-- ACMEhr: extend absences with type/status/note (vacation planning + sick days).
-- Existing (seeded) rows are approved vacations or treatment leaves.

alter table absence add column type varchar(16) not null default 'VACATION';
alter table absence add column status varchar(16) not null default 'APPROVED';
alter table absence add column note varchar(512);

update absence set type = 'CURE' where reason_key like 'kur%';
