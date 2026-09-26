-- =====================================================================================
-- SEED DATA (seed profile only): actuarial reserves.
-- Accounts for the ULAE provision and the margin for adverse deviation (with the
-- reinsurers' share) and the accounting rules of the reserve events added in V420.
-- Every lookup is scoped by company (FVI). V960 copies the FVI chart to FVS later.
-- Reserve parameters, takaful settings and valuation runs are created at start-up by
-- ReservesSeedData through the services.
-- =====================================================================================
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, a.code, a.name, a.cls, 'SUB', p.id, cat.id, true,
    false, 'NONE', false, false, false, false, false, false, a.grp, date '2010-01-01', 'ACTIVE',
    'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
join (values
    ('1304', 'Reinsurers Share of Risk Margin', 'ASSET', '1300', 'RECV', 'Reinsurance Assets'),
    ('2105', 'Claims Reserve - ULAE', 'LIABILITY', '2100', 'TECH', 'Insurance Contract Liabilities'),
    ('2106', 'Claims Reserve - Margin for Adverse Deviation', 'LIABILITY', '2100', 'TECH',
     'Insurance Contract Liabilities')
) as a(code, name, cls, parent, category, grp) on true
join coa_account p on p.company_id = c.id and p.code = a.parent
join coa_category cat on cat.code = a.category
where c.code = 'FVI';

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('CLAIM_MARGIN_PROVISION', 'Standard ULAE and risk margin provision'),
        ('PREMIUM_DEFICIENCY_PROVISION', 'Standard premium deficiency reserve')) as e(code, name)
where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, false
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI',
(values
  (1, 'DEBIT', '5200', 'ULAE_CHANGE'),
  (2, 'CREDIT', '2105', 'ULAE_CHANGE'),
  (3, 'DEBIT', '5200', 'MFAD_CHANGE'),
  (4, 'CREDIT', '2106', 'MFAD_CHANGE'),
  (5, 'DEBIT', '1304', 'RI_MFAD_CHANGE'),
  (6, 'CREDIT', '5300', 'RI_MFAD_CHANGE')
) as l(n, side, acc, comp)
where r.event_type = 'CLAIM_MARGIN_PROVISION';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, false
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI',
(values
  (1, 'DEBIT', '4300', 'PDR_CHANGE'),
  (2, 'CREDIT', '2104', 'PDR_CHANGE')
) as l(n, side, acc, comp)
where r.event_type = 'PREMIUM_DEFICIENCY_PROVISION';
