-- =====================================================================================
-- iNXT BrokerVerse - V982 Seed catalog data (seed profile only): a fictional insurer panel
-- (names invented for the seed, no real insurers), branches with their LGT rates,
-- commission rates, NB cost centers and the sales organisation of the seed account officers.
-- =====================================================================================
insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, 'INSURER', p.tin, p.addr, p.email, null, 'PHP', p.days, 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from org_company c, (values
  ('INS-MGIC', 'Mabuhay General Insurance Corp.', '500-111-222-000', 'Makati', 'placement@mabuhaygeneral.example', 30),
  ('INS-LAC', 'Luzon Assurance Co.', '501-111-222-000', 'Quezon City', 'placements@luzonassurance.example', 45),
  ('INS-VMI', 'Visayas Mutual Insurance', '502-111-222-000', 'Iloilo City', 'nb@visayasmutual.example', 30),
  ('INS-MPI', 'Mindanao Pacific Insurance Inc.', '503-111-222-000', 'Davao City', 'underwriting@mindanaopacific.example', 60)
) as p(code, name, tin, addr, email, days)
where c.code = 'FVI';

insert into cat_insurer (company_id, party_code, name, short_name, accreditation_no, accredited_until,
    placement_channel, placement_emails, default_credit_days, record_status, authorized_by,
    authorized_at, created_at, created_by)
select c.id, i.code, i.name, i.short, i.acc, i.until::date, 'EMAIL', i.email, i.days, 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from org_company c, (values
  ('INS-MGIC', 'Mabuhay General Insurance Corp.', 'Mabuhay General', 'IC-NL-2019-041', '2027-12-31', 'placement@mabuhaygeneral.example', 30),
  ('INS-LAC', 'Luzon Assurance Co.', 'Luzon Assurance', 'IC-NL-2018-022', '2027-06-30', 'placements@luzonassurance.example', 45),
  ('INS-VMI', 'Visayas Mutual Insurance', 'Visayas Mutual', 'IC-NL-2020-015', '2026-12-31', 'nb@visayasmutual.example', 30),
  ('INS-MPI', 'Mindanao Pacific Insurance Inc.', 'Mindanao Pacific', 'IC-NL-2021-008', '2027-03-31', 'underwriting@mindanaopacific.example', 60)
) as i(code, name, short, acc, until, email, days)
where c.code = 'FVI';

insert into cat_insurer_branch (insurer_id, code, name, city, lgt_rate, placement_email, record_status,
    authorized_by, authorized_at, created_at, created_by)
select i.id, b.code, b.name, b.city, b.lgt, b.email, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from cat_insurer i join org_company c on c.id = i.company_id and c.code = 'FVI'
join (values
  ('INS-MGIC', 'MKT', 'Makati Head Office', 'Makati', 0.75, 'uw.makati@mabuhaygeneral.example'),
  ('INS-MGIC', 'CEB', 'Cebu Branch', 'Cebu City', 0.50, 'uw.cebu@mabuhaygeneral.example'),
  ('INS-MGIC', 'DVO', 'Davao Branch', 'Davao City', 0.60, null),
  ('INS-LAC', 'QC', 'Quezon City Head Office', 'Quezon City', 0.75, null),
  ('INS-LAC', 'BAG', 'Baguio Branch', 'Baguio City', 0.20, null),
  ('INS-VMI', 'ILO', 'Iloilo Head Office', 'Iloilo City', 0.50, null),
  ('INS-VMI', 'CEB', 'Cebu Branch', 'Cebu City', 0.50, null),
  ('INS-MPI', 'DVO', 'Davao Head Office', 'Davao City', 0.60, null)
) as b(insurer, code, name, city, lgt, email) on b.insurer = i.party_code;

insert into cat_commission_rate (company_id, insurer_code, product_code, rate, effective_from,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, r.insurer, r.product, r.rate, date '2026-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('INS-MGIC', null, 20),
  ('INS-MGIC', 'MTR10', 17.5),
  ('INS-MGIC', 'PAR01', 25),
  ('INS-LAC', null, 18),
  ('INS-LAC', 'PAR08', 22.5),
  ('INS-VMI', null, 15),
  ('INS-MPI', null, 16)
) as r(insurer, product, rate)
where c.code = 'FVI';

-- Cost centers of the New Business sales teams (BRNB.108) and the seed sales organisation.
insert into dim_value (company_id, dimension_type, code, name, created_at, created_by)
select c.id, 'COST_CENTER', d.code, d.name, now(), 'SYSTEM'
from org_company c, (values ('NB-CBG-M', 'NB CBG Metro Manila'), ('NB-CBG-V', 'NB CBG Visayas'),
                            ('NB-CORP', 'NB Corporate Accounts')) as d(code, name)
where c.code = 'FVI';

insert into cat_sales_unit (company_id, unit_level, code, name, parent_code, cost_center, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, u.lvl, u.code, u.name, u.parent, u.cc, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('REGION', 'NCR', 'National Capital Region', null, null),
  ('REGION', 'VIS', 'Visayas', null, null),
  ('DEPARTMENT', 'CBG-NCR', 'Consumer Banking - NCR', 'NCR', null),
  ('DEPARTMENT', 'CORP-NCR', 'Corporate Accounts - NCR', 'NCR', null),
  ('DEPARTMENT', 'CBG-VIS', 'Consumer Banking - Visayas', 'VIS', null),
  ('TEAM', 'T-CBG1', 'CBG Metro Team 1', 'CBG-NCR', 'NB-CBG-M'),
  ('TEAM', 'T-CORP1', 'Corporate Team 1', 'CORP-NCR', 'NB-CORP'),
  ('TEAM', 'T-VIS1', 'CBG Cebu Team', 'CBG-VIS', 'NB-CBG-V')
) as u(lvl, code, name, parent, cc)
where c.code = 'FVI';

insert into cat_sales_officer (company_id, team_code, username, record_status, authorized_by,
    authorized_at, created_at, created_by)
select c.id, o.team, o.username, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values ('T-CBG1', 'ao'), ('T-CORP1', 'ao2'), ('T-CBG1', 'mkttl')) as o(team, username)
where c.code = 'FVI';
