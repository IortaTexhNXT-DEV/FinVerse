-- =====================================================================================
-- iNXT BrokerVerse - V1150 Platform client profile (range V1150-V1159, removal of client-specific
-- values from the platform). The company master carries the identity of the client that documents,
-- screens and client-facing labels use, so no client name, code or logo is written into the
-- platform:
--   short_name        short name of the company in texts and labels ("Via <short name>")
--   group_name        name of the group the company belongs to, used in labels of group concepts
--                     ("<group> bank client", "<group> branch"); blank when the company has none
--   logo_ref          reference of the logo printed on documents (name of the theme pack logo or
--                     a file store reference); blank = the logo of the deployed theme pack
--   head_office_code  code that stands for the head office in files exchanged with the client and
--                     for records without a branch (defaults to the code of the head-office branch)
--   default_bank_code bank account code proposed by default where a report or a form asks for the
--                     company bank account (payment notification)
-- Existing companies get their code as short name and the code of their head-office branch.
-- =====================================================================================

alter table org_company add column short_name varchar(30);
alter table org_company add column group_name varchar(60);
alter table org_company add column logo_ref varchar(300);
alter table org_company add column head_office_code varchar(10);
alter table org_company add column default_bank_code varchar(30);

update org_company c set short_name = c.code where c.short_name is null;

update org_company c
set head_office_code = (select b.code from org_branch b
                        where b.company_id = c.id and b.head_office
                        order by b.id limit 1)
where c.head_office_code is null;

alter table org_company alter column short_name set not null;

-- A company created without a short name takes its code, as the existing companies above.
create or replace function org_company_default_short_name() returns trigger language plpgsql as $$
begin
  if new.short_name is null or btrim(new.short_name) = '' then
    new.short_name := new.code;
  end if;
  return new;
end;
$$;

create trigger org_company_short_name_default
  before insert or update of short_name on org_company
  for each row execute function org_company_default_short_name();
