-- =====================================================================================
-- SEED DATA (seed profile only): claims.
-- Claim documents (registrations, reserve changes, settlements, recoveries, LPOs) are
-- created at start-up by ClaimsSeedData through the services, so journals, open items,
-- movement lines and the accounting event register are real. This script adds what the
-- services need in the seed company FVI only:
--   * checker roles: the finance manager ("fmanager", unlimited) and the authorizer
--     ("checker", limit 5,000,000) approve reserves and settlements entered by "claims";
--   * claim parties: surveyors / adjusters, third-party claimants and a salvage buyer;
--   * the accounting rule of CLAIM_COINSURANCE (coinsurers' share when leading).
-- =====================================================================================

insert into sec_role_permission (role_id, permission)
select r.id, p from sec_role r, unnest(array['CLAIM_AUTHORIZE']) as p
where r.code in ('FIN_MANAGER', 'AUTHORIZER')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p);

insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, p.t, p.tin, p.addr, p.email, p.phone, 'PHP', p.days,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('SV-0001', 'Philippine Adjusters and Surveyors Inc.', 'SURVEYOR', '903-111-222-000', 'Makati City',
   'survey@pasi.example', '+63 2 333 1001', 15),
  ('SV-0002', 'Visayas Loss Adjusters Co.', 'SURVEYOR', '904-111-222-000', 'Cebu City',
   'adjust@vla.example', '+63 32 333 1002', 15),
  ('TP-0001', 'Ramon Bautista', 'INDIVIDUAL_CLIENT', '124-456-789-000', 'Mandaluyong City',
   'ramon.bautista@example.ph', '+63 917 000 2001', 0),
  ('TP-0002', 'Liza Villanueva', 'INDIVIDUAL_CLIENT', '125-456-789-000', 'Cebu City',
   'liza.villanueva@example.ph', '+63 917 000 2002', 0),
  ('S-0010', 'Metro Salvage Traders', 'SUPPLIER', '805-111-222-000', 'Valenzuela City',
   'buy@metrosalvage.example', '+63 2 444 0010', 30)
) as p(code, name, t, tin, addr, email, phone, days)
where c.code = 'FVI'
  and not exists (select 1 from pty_party x where x.company_id = c.id and x.code = p.code);

-- Coinsurance of claims led by the company: coinsurers' share of a settlement is recoverable
-- from them (Dr 1206 / Cr 2204), their share of a recovery is payable to them (Dr bank / Cr 2203).
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'CLAIM_COINSURANCE', 'Standard claim coinsurance share', 100, date '2010-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1206', 'COINSURER_SHARE', true),
  (2, 'CREDIT', '2204', 'COINSURER_SHARE', false),
  (3, 'DEBIT', '@BANK', 'COINSURER_RECOVERY', false),
  (4, 'CREDIT', '2203', 'COINSURER_RECOVERY', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'CLAIM_COINSURANCE'
  and r.company_id = (select id from org_company where code = 'FVI');
