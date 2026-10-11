-- Realistic display data for the full-page screenshots of the BDOI-template FRS (capture_pack.cjs with FULL_PAGE=1):
-- run once on the fresh capture database before the shots. The persona and test users of the seed get a first name
-- and a surname (no role words, no "SIT"), their e-mail addresses follow the first name and surname, and the request
-- names, batch file name and remarks of the seed lose their test wording. The sign-in IDs (usernames) do not change.
-- Every text column of the schema that holds an old value is updated (audit and history copies included).
create temp table rn (old text primary key, new text not null);

insert into rn values
  ('Rhea Access Requestor', 'Rhea Santiago'), ('Ulysses Access Approver', 'Ulysses Abad'),
  ('Selina Second Approver', 'Selina Cruz'), ('SIT System Administrator', 'Samuel Villanueva'),
  ('Audrey Auditor', 'Audrey Lim'), ('Aileen Account Officer', 'Aileen Torres'),
  ('Andres Approver', 'Andres Bautista'), ('Bea Business Admin', 'Bea Garcia'),
  ('Ingrid Security Officer', 'Ingrid Mendoza'), ('SIT Enrolled User', 'Maria Santos'),
  ('SIT Leaver', 'Carlo Dizon'), ('SIT Transferee', 'Liza Fernandez'), ('SIT Team Lead', 'Ramon Aguilar'),
  ('SIT Draft User', 'Jessa Ocampo'), ('SIT Cancelled Hire', 'Noel Pascual'),
  ('SIT Bulk Officer 1', 'Grace Manalo'), ('SIT Bulk Officer 2', 'Paolo Soriano'), ('SIT Bulk Officer 3', 'Ana Lacson');

-- The other seed users: first name kept, the role words replaced by a surname (none starting with A, so that a
-- persona above stays first where a list is sorted by name).
with surnames as (
  select array['Belmonte','Cabrera','Castillo','Dela Cruz','Domingo','Espiritu','Estrada','Fajardo','Flores','Galang',
               'Gonzales','Hernandez','Herrera','Ignacio','Jimenez','Lopez','Mercado','Nepomuceno','Ocampo','Panganiban',
               'Quiambao','Quizon','Ramos','Reyes','Rivera','Salazar','Soriano','Tan','Tolentino','Umali','Uy','Valdez',
               'Vergara','Villareal','Yap','Zamora'] s),
others as (
  select u.full_name, split_part(u.full_name, ' ', 1) first_name, row_number() over (order by u.username) n
  from sec_user u
  where u.full_name is not null and u.full_name not in (select old from rn) and u.full_name like '% %')
insert into rn
select o.full_name, o.first_name || ' ' || s.s[1 + (o.n - 1) % array_length(s.s, 1)]
from others o, surnames s
on conflict do nothing;

-- E-mail addresses of the users: first.surname@ the DEV domain (unique by a number when two users share a name).
create temp table re (old text primary key, new text not null);
insert into re
select u.email,
       lower(regexp_replace(translate(coalesce(r.new, u.full_name), 'ñÑ', 'nN'), '[^A-Za-z]+', '.', 'g'))
         || case when row_number() over (partition by coalesce(r.new, u.full_name) order by u.username) > 1
                 then (row_number() over (partition by coalesce(r.new, u.full_name) order by u.username))::text else '' end
         || '@dev.brokerverse.cloud'
from sec_user u left join rn r on r.old = u.full_name
where u.email is not null
on conflict do nothing;
insert into rn select old, new from re on conflict do nothing;
insert into rn values ('uam-bulk-requests.xlsx', 'marketing-account-officers.xlsx'),
                      ('uam-bulk-seed.xlsx', 'marketing-account-officers.xlsx')
on conflict do nothing;

do $$
declare
  c record;
  r record;
  pattern text;
  hit boolean;
begin
  select string_agg(regexp_replace(old, '([.*+?^${}()|\[\]\\])', '\\\1', 'g'), '|') into pattern from rn;
  for c in
    select table_schema, table_name, column_name, data_type
    from information_schema.columns
    where table_schema = 'public' and data_type in ('text', 'character varying', 'jsonb')
      and table_name in (select table_name from information_schema.tables where table_type = 'BASE TABLE' and table_schema = 'public')
  loop
    execute format('select exists (select 1 from %I.%I where %I::text ~ $1)', c.table_schema, c.table_name, c.column_name)
      into hit using pattern;
    if hit then
      for r in select old, new from rn order by length(old) desc loop
        begin
          if c.data_type = 'jsonb' then
            execute format('update %I.%I set %I = replace(%I::text, $1, $2)::jsonb where strpos(%I::text, $1) > 0',
                           c.table_schema, c.table_name, c.column_name, c.column_name, c.column_name) using r.old, r.new;
          else
            execute format('update %I.%I set %I = replace(%I, $1, $2) where strpos(%I, $1) > 0',
                           c.table_schema, c.table_name, c.column_name, c.column_name, c.column_name) using r.old, r.new;
          end if;
        exception when others then
          raise notice 'skipped %.%: %', c.table_name, c.column_name, sqlerrm;
        end;
      end loop;
    end if;
  end loop;
end $$;
