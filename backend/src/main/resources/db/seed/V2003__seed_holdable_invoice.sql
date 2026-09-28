-- Seed correction found by the BRD-2 Operations business sign-off (seed profile only; runs after all
-- seed data): the seeded remittance hold request waited on the invoice that the seeded flat
-- cancellation locks for Adjustment, so the hold could not be approved. One more motor account in
-- POLICY_ISSUED (ARN-2026-940008, client CL-2026-000002) is booked by the hold seed at start-up
-- and carries the hold request, which the Marketing Team Leader can approve.

create temporary table bkg_seed_account_hold (
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

insert into bkg_seed_account_hold values
('ARN-2026-940008', 'CL-2026-000002', 'MTR10', 'MOTOR', 'COMPREHENSIVE', 'RETAIL', 'INS-MGIC', 'MKT',
 date '2026-09-12', date '2027-09-12', false, 1, 1250000.00, 15450.00, 1931.25, 0.00, 1854.00, 0.00, 115.88,
 3901.13, 19351.13, 17.5, 2703.75, 324.45, 'VIA_BDOI', 'CLIENT_CONFIRMED', 'NCR', 'CBG-NCR', 'T-CBG1', 'ao',
 'NB-CBG-M', 'VEHICLE', 'BKG9888', 1.30, 'MGIC-MC-2026-98808', date '2026-09-12',
 timestamptz '2026-08-29T09:00:00+08');

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
from bkg_seed_account_hold d
join org_company c on c.code = 'FVI'
join crm_client cl on cl.client_code = d.client_code;

insert into acc_account_policy (account_id, year_index, policy_number)
select a.id, p.ord - 1, p.policy
from bkg_seed_account_hold d
join acc_account a on a.arn = d.arn
cross join lateral unnest(string_to_array(d.policies, ',')) with ordinality as p(policy, ord);

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, plate_no,
    engine_no, chassis_no, make, model, year_model, body_type, sum_insured, rate, bi_limit, pd_limit)
select a.id, 1, d.net, d.created, d.officer, 'VEHICLE', d.item_label, 'E' || d.item_label, 'C' || d.item_label,
       'Toyota', 'Corolla Cross 1.8 V', 2026, 'SUV', d.tsi, d.rate, 100000, 100000
from bkg_seed_account_hold d join acc_account a on a.arn = d.arn
where d.item_kind = 'VEHICLE';

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, address, city,
    province, occupancy, construction_class, location_key, sum_insured, rate)
select a.id, 1, d.net, d.created, d.officer, 'PROPERTY_LOCATION', d.item_label, 'Pasig', 'Metro Manila',
       'DWELLING', 'CLASS_1', lower(d.item_label) || ' | pasig', d.tsi, d.rate
from bkg_seed_account_hold d join acc_account a on a.arn = d.arn
where d.item_kind = 'PROPERTY_LOCATION';

insert into acc_risk_item_detail (risk_item_id, detail_index, description, sum_insured)
select i.id, 0, 'Building', d.tsi
from bkg_seed_account_hold d
join acc_account a on a.arn = d.arn
join acc_risk_item i on i.account_id = a.id and i.item_no = 1
where d.item_kind = 'PROPERTY_LOCATION';

insert into acc_risk_item (account_id, item_no, premium, created_at, created_by, kind, description,
    sum_insured, rate)
select a.id, 1, d.net, d.created, d.officer, 'GENERIC', d.item_label, d.tsi, d.rate
from bkg_seed_account_hold d join acc_account a on a.arn = d.arn
where d.item_kind = 'GENERIC';

insert into wf_case (company_id, workflow_code, entity_type, entity_id, reference, title, link,
    originating_unit, stage_code, stage_entered_at, due_at, assignee, closed, created_at, created_by)
select a.company_id, 'NB_ACCOUNT', 'Account', a.id::text, a.arn, a.client_name || ' - ' || a.product_code,
       '/accounts/' || a.id, d.team, 'POLICY_ISSUED', d.issued::timestamptz + interval '9 hours',
       d.issued::timestamptz + interval '33 hours', null, false, d.created, d.officer
from bkg_seed_account_hold d join acc_account a on a.arn = d.arn;

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor,
    automatic, occurred_at)
select w.id, s.from_stage, s.to_stage, s.action, null, s.note, s.actor, s.auto, d.created + s.offset_days
from bkg_seed_account_hold d
join wf_case w on w.entity_type = 'Account' and w.reference = d.arn
cross join (values
    (null,                  'DRAFT',               'start',             null,                        'ao',     false, interval '0 days'),
    ('DRAFT',               'SUBMITTED',           'submit',            null,                        'ao',     false, interval '1 day'),
    ('SUBMITTED',           'AWAITING_PAYMENT',    'validate',          null,                        'proc',   false, interval '2 days'),
    ('AWAITING_PAYMENT',    'READY_FOR_PLACEMENT', 'payment_confirmed', 'Payment gate passed',       'SYSTEM', true,  interval '3 days'),
    ('READY_FOR_PLACEMENT', 'PLACED',              'place',             'Placement slip sent',       'proc',   false, interval '4 days'),
    ('PLACED',              'POLICY_ISSUED',       'policy_received',   'E-policy received',         'proc',   false, interval '10 days')
) as s(from_stage, to_stage, action, note, actor, auto, offset_days);

