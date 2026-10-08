-- =====================================================================================
-- iNXT BrokerVerse - V2500 Insurer suite removal (range V2500-V2509): Tax & Statutory.
-- BIBS is specific to BDOI as an insurance broker (client decision of 8 October 2026,
-- docs/development/CODEBASE_RELEVANCE_AUDIT.md T1). The insurer parts of Tax & Statutory are removed:
--   * the Insurance Commission schedules of an insurer and their account mapping (tax_ic_line_item,
--     V700);
--   * the premium levy worksheets (documentary stamp tax, premium tax, local government tax and fire
--     service tax on the insurer's own policies): their tax forms and returns are deleted, with the
--     lines and remittance records of those returns (posted remittance journals stay in the
--     ledger), and the form worksheet is limited to VAT, EWT and NONE.
-- The BIR outputs of the broker (VAT 2550Q, EWT 0619-E / 1601-EQ, 2307, books, IC-BROKER-ASBO), the
-- tax codes and the party tax profiles are kept.
-- Version: above every seed script (db/seed up to V2090), so on a new database the seed scripts run
-- while the insurer objects still exist.
-- =====================================================================================

drop table tax_ic_line_item;

create temporary table tmp_insurer_tax_return on commit drop as
select r.id
from tax_return r
where r.worksheet in ('DST', 'PREMIUM_TAX', 'LGT', 'FST')
   or exists (select 1 from tax_form f
              where f.company_id = r.company_id and f.code = r.form_code
                and f.worksheet in ('DST', 'PREMIUM_TAX', 'LGT', 'FST'));

delete from tax_remittance where return_id in (select id from tmp_insurer_tax_return);
delete from tax_return_line where return_id in (select id from tmp_insurer_tax_return);
delete from tax_return where id in (select id from tmp_insurer_tax_return);
delete from tax_form where worksheet in ('DST', 'PREMIUM_TAX', 'LGT', 'FST');

alter table tax_form drop constraint tax_ck_form_worksheet;
alter table tax_form add constraint tax_ck_form_worksheet check (worksheet in ('VAT', 'EWT', 'NONE'));
alter table tax_return add constraint tax_ck_return_worksheet check (worksheet in ('VAT', 'EWT', 'NONE'));
