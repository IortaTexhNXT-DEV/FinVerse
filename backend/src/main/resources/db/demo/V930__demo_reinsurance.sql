-- =====================================================================================
-- DEMO DATA (demo profile only): reinsurance.
-- Two additional Asian reinsurers and a reinsurance broker for the demo company, and the
-- accounting rules of the reinsurance statement events. Treaties, cessions, facultative
-- placements and statements of account are created at start-up by ReinsuranceDemoData
-- through the services, so journals and open items are real.
-- Every lookup is scoped to the demo company FVI (the subsidiary FVS has no reinsurance).
-- =====================================================================================
insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, commission_rate, withholding_tax_rate, licence_no,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, p.t, p.tin, p.addr, p.email, p.phone, p.ccy, p.days, null, null, p.lic,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('R-0004', 'Pan Asian Reinsurance (Demo)', 'REINSURER', null, 'Hong Kong',
   'treaty@panasianre.example', '+852 3000 0004', 'PHP', 90, null),
  ('R-0005', 'Asia Capital Reinsurance (Demo)', 'REINSURER', null, 'Singapore',
   'treaty@asiacapre.example', '+65 6000 0005', 'PHP', 90, null),
  ('RB-0001', 'Manila Reinsurance Brokers (Demo)', 'RI_BROKER', '602-111-222-000', 'Makati City',
   'placing@manilare.example', '+63 2 888 0101', 'PHP', 60, 'IC-RB-2016-0012')
) as p(code, name, t, tin, addr, email, phone, ccy, days, lic)
where c.code = 'FVI'
  and not exists (select 1 from pty_party x where x.company_id = c.id and x.code = p.code);

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('RI_SOA_ADJUSTMENT', 'Standard ri statement adjustments'),
  ('RI_BALANCE_OFFSET', 'Standard ri balance offset')
) as e(code, name)
where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI',
(values
  (1, 'DEBIT', '2201', 'LEVY', true),
  (2, 'CREDIT', '2507', 'LEVY', false),
  (3, 'DEBIT', '2201', 'PREMIUM_RESERVE', true),
  (4, 'CREDIT', '2202', 'PREMIUM_RESERVE', false),
  (5, 'DEBIT', '2201', 'LOSS_RESERVE', true),
  (6, 'CREDIT', '2202', 'LOSS_RESERVE', false),
  (7, 'DEBIT', '4200', 'INTEREST', false),
  (8, 'CREDIT', '2201', 'INTEREST', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_SOA_ADJUSTMENT';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI',
(values
  (1, 'DEBIT', '2201', 'AMOUNT', true),
  (2, 'CREDIT', '1205', 'AMOUNT', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_BALANCE_OFFSET';
