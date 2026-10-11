-- =====================================================================================
-- iNXT BrokerVerse - V988 Seed booking (seed profile only).
--   * Seed GL accounts of the broker booking entry (OPERATIONS_DESIGN section 5 numbering),
--     only where missing: 1210 PR with component sub-accounts, 1220, 2210, 2220, 2221, 4101.
--   * BUSINESS_LINE dimension values for the catalog product lines (the booking journal
--     carries the product line as its line of business).
--   * Seed accounting rule of BROKER_BOOKING (real accounts come from Comptrollership, OQ07).
--   * One seed incentive rule (Q33 parked) and one seed auto-book rule (BRNB.076).
--   * Seven accounts in POLICY_ISSUED for seed clients CL-2026-000001..006 (V981). The seed
--     seeder (booking.seed.BookingSeedData) books four of them through the real booking
--     service at start-up, posts a positive endorsement and a partial cancellation; one is
--     queued for the end-of-day batch and two (one multi-year) stay ready to book.
-- =====================================================================================

-- ---------- Seed GL accounts ---------------------------------------------------------------
create temporary table bkg_seed_gl (
    code varchar(30), name varchar(120), account_class varchar(20), level varchar(10),
    parent varchar(30), category varchar(10), postable boolean, control boolean,
    sub_ledger varchar(20), report_group varchar(120), seq integer
) on commit drop;

insert into bkg_seed_gl values
    ('1210',    'Premium Receivable - Clients',                'ASSET',     'MAIN', '1200', 'RECV', false, false, 'NONE',         'Insurance Receivables', 1),
    ('1210.01', 'Premium Receivable - Basic Premium',          'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 2),
    ('1210.02', 'Premium Receivable - DST',                    'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 3),
    ('1210.03', 'Premium Receivable - Premium Tax / VAT',      'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 4),
    ('1210.04', 'Premium Receivable - LGT',                    'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 5),
    ('1210.05', 'Premium Receivable - Fire Service Tax',       'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 6),
    ('1210.06', 'Premium Receivable - Other Charges',          'ASSET',     'SUB',  '1210', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 7),
    ('1220',    'Commission Receivable - Insurers',            'ASSET',     'SUB',  '1200', 'RECV', true,  true,  'INSURER',      'Insurance Receivables', 8),
    ('2210',    'Due to Insurers (DTIP)',                      'LIABILITY', 'SUB',  '2200', 'PAYB', true,  true,  'INSURER',      'Insurance Payables', 9),
    ('2220',    'Unrealized Commission',                       'LIABILITY', 'SUB',  '2200', 'PAYB', true,  false, 'NONE',         'Insurance Payables', 10),
    ('2221',    'Deferred Output VAT',                         'LIABILITY', 'SUB',  '2500', 'TAXP', true,  false, 'NONE',         'Accounts Payable and Accrued Expenses', 11),
    ('4101',    'Commission Income - Brokerage',               'INCOME',    'MAIN', '4000', 'PREM', true,  false, 'NONE',         'Commission Income', 12);

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, g.code, g.name, g.account_class, g.level,
       (select p.id from coa_account p where p.company_id = c.id and p.code = g.parent),
       (select id from coa_category where code = g.category), g.postable, g.control, g.sub_ledger,
       false, false, false, false, false, false, g.report_group, date '2026-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (select * from bkg_seed_gl order by seq) g
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = g.code)
order by g.seq;

-- ---------- Lines of business of the catalog product lines -----------------------------------
insert into dim_value (company_id, dimension_type, code, name, created_at, created_by)
select c.id, 'BUSINESS_LINE', l.code, l.name, now(), 'SYSTEM'
from org_company c
cross join cat_product_line l
where c.code = 'FVI'
  and not exists (select 1 from dim_value d where d.company_id = c.id
                  and d.dimension_type = 'BUSINESS_LINE' and d.code = l.code);

-- ---------- Seed accounting rule of BROKER_BOOKING (OPERATIONS_DESIGN section 5, row 0) ------
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'BROKER_BOOKING', 'Broker booking - premium receivable, DTIP and commission (seed)', 100,
       date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  (1,  'DEBIT',  '1210.01', 'PR_BASIC',              true,  'Premium receivable - basic premium'),
  (2,  'DEBIT',  '1210.02', 'PR_DST',                true,  'Premium receivable - DST'),
  (3,  'DEBIT',  '1210.03', 'PR_PTX_VAT',            true,  'Premium receivable - premium tax / VAT'),
  (4,  'DEBIT',  '1210.04', 'PR_LGT',                true,  'Premium receivable - LGT'),
  (5,  'DEBIT',  '1210.05', 'PR_FST',                true,  'Premium receivable - fire service tax'),
  (6,  'DEBIT',  '1210.06', 'PR_OTHER',              true,  'Premium receivable - other charges'),
  (7,  'CREDIT', '2210',    'DTIP',                  true,  'Due to insurer'),
  (8,  'DEBIT',  '1220',    'COMMISSION_RECEIVABLE', true,  'Commission receivable (with VAT)'),
  (9,  'CREDIT', '2220',    'UNREALIZED_COMMISSION', false, 'Unrealized commission'),
  (10, 'CREDIT', '2221',    'DEFERRED_OUTPUT_VAT',   false, 'Deferred output VAT on commission'),
  (11, 'CREDIT', '4101',    'COMMISSION_INCOME',     false, 'Commission income'),
  (12, 'CREDIT', '2504',    'OUTPUT_VAT',            false, 'Output VAT on commission')
) as l(n, side, acc, comp, party, narr)
where r.event_type = 'BROKER_BOOKING';

-- ---------- Seed rules --------------------------------------------------------------------
insert into bkg_incentive_rule (company_id, product_code, market_segment, source_channel, period_from,
    period_to, active, description, created_at, created_by)
select id, 'MTR10', 'CBG', null, date '2026-01-01', date '2026-12-31', true,
       'Placeholder: CBG motor MTR10 campaign 2026 (qualification rules pending, Q33)', now(), 'SYSTEM'
from org_company where code = 'FVI';

insert into bkg_auto_book_rule (company_id, product_code, market_segment, enabled, description,
    created_at, created_by)
select id, 'PAR08', 'CBG', true, 'Placeholder: CBG fire PAR08 is booked by the end-of-day batch', now(), 'SYSTEM'
from org_company where code = 'FVI';

-- ---------- Seed accounts in POLICY_ISSUED ---------------------------------------------------
create temporary table bkg_seed_account (
    arn varchar(30), client_code varchar(30), product varchar(20), line varchar(30), cover varchar(30),
    segment varchar(40), insurer varchar(30), branch varchar(20), period_from date, period_to date,
    multi_year boolean, term_years integer, tsi numeric(19, 2), net numeric(19, 2), dst numeric(19, 2),
    ptx numeric(19, 2), vat numeric(19, 2), fst numeric(19, 2), lgt numeric(19, 2),
    charges numeric(19, 2), gross numeric(19, 2), comm_rate numeric(19, 8), comm numeric(19, 2),
    comm_vat numeric(19, 2), arrangement varchar(20), payment_status varchar(20), region varchar(20),
    department varchar(20), team varchar(20), officer varchar(50), cost_center varchar(20),
    item_kind varchar(30), item_label varchar(200), rate numeric(19, 8), policies varchar(200),
    issued date, created timestamptz
) on commit drop;

insert into bkg_seed_account values
('ARN-2026-940001', 'CL-2026-000001', 'MTR10', 'MOTOR', 'COMPREHENSIVE', 'CBG', 'INS-MGIC', 'MKT',
 date '2026-09-01', date '2027-09-01', false, 1, 1100000.00, 13595.00, 1699.50, 0.00, 1631.40, 0.00, 101.96,
 3432.86, 17027.86, 17.5, 2379.13, 285.50, 'VIA_BDOI', 'PAID', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao', 'NB-CBG-M',
 'VEHICLE', 'BKG9881', 1.30, 'MGIC-MC-2026-98801', date '2026-09-08', timestamptz '2026-08-25T09:00:00+08'),
('ARN-2026-940002', 'CL-2026-000005', 'PAR01', 'PROPERTY', 'FIRE_LIGHTNING', 'CBG', 'INS-MGIC', 'MKT',
 date '2026-09-01', date '2027-09-01', false, 1, 7000000.00, 17500.00, 2187.50, 2100.00, 0.00, 350.00, 131.25,
 4768.75, 22268.75, 25, 4375.00, 525.00, 'VIA_BDOI', 'PAID', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao', 'NB-CBG-M',
 'PROPERTY_LOCATION', '9 Booking Street, Ortigas Center', 0.25, 'MGIC-FI-2026-98802', date '2026-09-08',
 timestamptz '2026-08-25T10:00:00+08'),
('ARN-2026-940003', 'CL-2026-000001', 'MTR10', 'MOTOR', 'COMPREHENSIVE', 'CBG', 'INS-MGIC', 'CEB',
 date '2026-09-05', date '2027-09-05', false, 1, 1850000.00, 22805.00, 2851.00, 0.00, 2736.60, 0.00, 114.03,
 5701.63, 28506.63, 17.5, 3990.88, 478.91, 'DIRECT_TO_INSURER', 'DIRECT', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao',
 'NB-CBG-M', 'VEHICLE', 'BKG9883', 1.30, 'MGIC-MC-2026-98803', date '2026-09-09', timestamptz '2026-08-26T09:00:00+08'),
('ARN-2026-940004', 'CL-2026-000003', 'CGL01', 'LIABILITY', 'CGL', 'CORBANK', 'INS-LAC', 'QC',
 date '2026-09-10', date '2027-09-10', false, 1, 25000000.00, 62500.00, 7812.50, 0.00, 7500.00, 0.00, 468.75,
 15781.25, 78281.25, 18, 11250.00, 1350.00, 'VIA_BDOI', 'CLIENT_CONFIRMED', 'NCR', 'CORP-NCR', 'T-CORP1', 'ao2',
 'NB-CORP', 'GENERIC', 'Warehouse operations, 3 Harbor Drive, Port Area', 0.25, 'LAC-GL-2026-98804',
 date '2026-09-10', timestamptz '2026-08-27T09:00:00+08'),
('ARN-2026-940005', 'CL-2026-000002', 'MTR10', 'MOTOR', 'COMPREHENSIVE', 'RETAIL', 'INS-MGIC', 'MKT',
 date '2026-09-15', date '2027-09-15', false, 1, 1100000.00, 13595.00, 1699.50, 0.00, 1631.40, 0.00, 101.96,
 3432.86, 17027.86, 17.5, 2379.13, 285.50, 'VIA_BDOI', 'PAID', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao', 'NB-CBG-M',
 'VEHICLE', 'BKG9885', 1.30, 'MGIC-MC-2026-98805', date '2026-09-16', timestamptz '2026-09-01T09:00:00+08'),
('ARN-2026-940006', 'CL-2026-000006', 'PAR08', 'PROPERTY', 'FIRE_LIGHTNING', 'CBG', 'INS-LAC', 'QC',
 date '2026-09-20', date '2027-09-20', false, 1, 4200000.00, 10500.00, 1312.50, 1260.00, 0.00, 210.00, 78.75,
 2861.25, 13361.25, 22.5, 2362.50, 283.50, 'VIA_BDOI', 'PAID', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao', 'NB-CBG-M',
 'PROPERTY_LOCATION', '12 Booking Road, Lahug', 0.25, 'LAC-FI-2026-98806', date '2026-09-18',
 timestamptz '2026-09-02T09:00:00+08'),
('ARN-2026-940007', 'CL-2026-000005', 'PAR01', 'PROPERTY', 'FIRE_LIGHTNING', 'CBG', 'INS-MGIC', 'MKT',
 date '2026-09-01', date '2029-09-01', true, 3, 7000000.00, 17500.00, 2187.50, 2100.00, 0.00, 350.00, 131.25,
 4768.75, 22268.75, 25, 4375.00, 525.00, 'VIA_BDOI', 'PAID', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao', 'NB-CBG-M',
 'PROPERTY_LOCATION', '27 Multi-Year Avenue, Kapitolyo', 0.25,
 'MGIC-FI-2026-98807,MGIC-FI-2027-98807,MGIC-FI-2028-98807', date '2026-09-11', timestamptz '2026-08-28T09:00:00+08');

insert into acc_account (company_id, arn, client_id, client_code, client_name, product_code, line_code,
    cover_type_code, market_segment, source_channel, insurer_code, insurer_branch, period_from, period_to,
    multi_year, term_years, currency, total_sum_insured, rating_basis, net_premium, dst, premium_tax, vat,
    fst, lgt, total_charges, gross_premium, commission_rate, commission, vat_on_commission, minimum_applied,
    payment_arrangement, direct_payment_tagged_by, direct_payment_tagged_at, contact_name, contact_email,
    contact_mobile, sales_region, sales_department, sales_team, account_officer, cost_center, status,
    payment_status, payment_source, payment_confirmed_at, placement_slip_ref, placed_at, insurer_ref,
    policy_issue_date, epolicy_received, created_at, created_by)
select c.id, d.arn, cl.id, cl.client_code, cl.display_name, d.product, d.line, d.cover, d.segment, 'EMAIL',
       d.insurer, d.branch, d.period_from, d.period_to, d.multi_year, d.term_years, 'PHP', d.tsi, 'ANNUAL',
       d.net, d.dst, d.ptx, d.vat, d.fst, d.lgt, d.charges, d.gross, d.comm_rate, d.comm, d.comm_vat, false,
       d.arrangement,
       case when d.arrangement = 'DIRECT_TO_INSURER' then d.officer end,
       case when d.arrangement = 'DIRECT_TO_INSURER' then d.created end,
       cl.display_name, cl.email, cl.mobile, d.region, d.department, d.team, d.officer, d.cost_center,
       'POLICY_ISSUED', d.payment_status,
       case d.payment_status when 'DIRECT' then 'Direct payment to insurer'
            when 'CLIENT_CONFIRMED' then 'Client confirmation e-mail' else 'Payment report' end,
       d.created + interval '3 days', 'PL-2026-9' || right(d.arn, 5), d.created + interval '4 days',
       'REF-' || right(d.arn, 5), d.issued, true, d.created, d.officer
from bkg_seed_account d
join org_company c on c.code = 'FVI'
join crm_client cl on cl.client_code = d.client_code;

insert into acc_account_policy (account_id, year_index, policy_number)
select a.id, p.ord - 1, p.policy
from bkg_seed_account d
join acc_account a on a.arn = d.arn
cross join lateral unnest(string_to_array(d.policies, ',')) with ordinality as p(policy, ord);

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, plate_no,
    engine_no, chassis_no, make, model, year_model, body_type, sum_insured, rate, bi_limit, pd_limit)
select a.id, 1, d.net, d.created, d.officer, 'VEHICLE', d.item_label, 'E' || d.item_label, 'C' || d.item_label,
       'Toyota', 'Corolla Cross 1.8 V', 2026, 'SUV', d.tsi, d.rate, 100000, 100000
from bkg_seed_account d join acc_account a on a.arn = d.arn
where d.item_kind = 'VEHICLE';

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, address, city,
    province, occupancy, construction_class, location_key, sum_insured, rate)
select a.id, 1, d.net, d.created, d.officer, 'PROPERTY_LOCATION', d.item_label, 'Pasig', 'Metro Manila',
       'DWELLING', 'CLASS_1', lower(d.item_label) || ' | pasig', d.tsi, d.rate
from bkg_seed_account d join acc_account a on a.arn = d.arn
where d.item_kind = 'PROPERTY_LOCATION';

insert into acc_risk_item_detail (risk_item_id, detail_index, description, sum_insured)
select i.id, 0, 'Building', d.tsi
from bkg_seed_account d
join acc_account a on a.arn = d.arn
join acc_risk_item i on i.account_id = a.id and i.item_no = 1
where d.item_kind = 'PROPERTY_LOCATION';

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, description,
    sum_insured, rate)
select a.id, 1, d.net, d.created, d.officer, 'GENERIC', d.item_label, d.tsi, d.rate
from bkg_seed_account d join acc_account a on a.arn = d.arn
where d.item_kind = 'GENERIC';

insert into wf_case (company_id, workflow_code, entity_type, entity_id, reference, title, link,
    originating_unit, stage_code, stage_entered_at, due_at, assignee, closed, created_at, created_by)
select a.company_id, 'NB_ACCOUNT', 'Account', a.id::text, a.arn, a.client_name || ' - ' || a.product_code,
       '/accounts/' || a.id, d.team, 'POLICY_ISSUED', d.issued::timestamptz + interval '9 hours',
       d.issued::timestamptz + interval '33 hours', null, false, d.created, d.officer
from bkg_seed_account d join acc_account a on a.arn = d.arn;

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor,
    automatic, occurred_at)
select w.id, s.from_stage, s.to_stage, s.action, null, s.note, s.actor, s.auto, d.created + s.offset_days
from bkg_seed_account d
join wf_case w on w.entity_type = 'Account' and w.reference = d.arn
cross join (values
    (null,                  'DRAFT',               'start',             null,                        'ao',     false, interval '0 days'),
    ('DRAFT',               'SUBMITTED',           'submit',            null,                        'ao',     false, interval '1 day'),
    ('SUBMITTED',           'AWAITING_PAYMENT',    'validate',          null,                        'proc',   false, interval '2 days'),
    ('AWAITING_PAYMENT',    'READY_FOR_PLACEMENT', 'payment_confirmed', 'Payment gate passed',       'SYSTEM', true,  interval '3 days'),
    ('READY_FOR_PLACEMENT', 'PLACED',              'place',             'Placement slip sent',       'proc',   false, interval '4 days'),
    ('PLACED',              'POLICY_ISSUED',       'policy_received',   'E-policy received',         'proc',   false, interval '10 days')
) as s(from_stage, to_stage, action, note, actor, auto, offset_days);

-- Queued for the end-of-day batch by the seed auto-book rule (PAR08 CBG).
insert into bkg_queue (company_id, arn, account_id, status, source, queued_by, queued_at, created_at,
    created_by)
select a.company_id, a.arn, a.id, 'QUEUED', 'AUTO', 'proc', now(), now(), 'proc'
from acc_account a where a.arn = 'ARN-2026-940006';
