-- =====================================================================================
-- iNXT BrokerVerse - V993 Demo production reconciliation (demo profile only). DEMO ONLY.
--   * Demo values of the lists BDOI has still to provide (OQ31): company concerned
--     (PRCID.016, with "Others") and dispositions (PRCID.038/039). Production keeps them empty.
--   * Demo extraction schedules for the demo insurers (frequency parked, OQ29): monthly on
--     the 5th, not sent automatically.
--   The cycle itself is demo data of prodrecon.demo.ProdReconDemoData: after the demo
--   bookings reach the Operations ledger, the register of INS-MGIC for the booking month is
--   extracted through the real extraction service.
-- =====================================================================================

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    ('RECON_COMPANY_CONCERNED', 'BDOI', 'BDOI (booking)', 10),
    ('RECON_COMPANY_CONCERNED', 'INSURER', 'Insurer', 20),
    ('RECON_COMPANY_CONCERNED', 'CLIENT', 'Client', 30),
    ('RECON_COMPANY_CONCERNED', 'OTHERS', 'Others', 90),
    ('RECON_DISPOSITION', 'FOR_BOOKING', 'For booking by BDOI', 10),
    ('RECON_DISPOSITION', 'BDOI_TO_CORRECT', 'BDOI to correct the booking', 20),
    ('RECON_DISPOSITION', 'INSURER_TO_CORRECT', 'Insurer to correct its report', 30),
    ('RECON_DISPOSITION', 'FOR_CANCELLATION', 'For cancellation', 40),
    ('RECON_DISPOSITION', 'NOT_BDOI_ACCOUNT', 'Not a BDOI account', 50),
    ('RECON_DISPOSITION', 'FOR_CLOSURE', 'For closure', 90)
) as v(type_code, code, label, sort_order)
on conflict (type_code, code) do nothing;

insert into prc_schedule (company_id, insurer_code, frequency, run_day, next_run_date, auto_send, recipients,
                          active, created_at, created_by)
select c.id, i.code, 'MONTHLY', 5, date '2026-10-05', false, null, true, now(), 'SYSTEM'
from org_company c
cross join (values ('INS-MGIC'), ('INS-LAC')) as i(code)
where c.code = 'FVI'
on conflict (company_id, insurer_code) do nothing;
