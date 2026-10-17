-- Renewal extraction, duplicate checking, sanitation and automatic updates of BDOI's Renewal FRS
-- (FRRN.004 to FRRN.009): the settings of BDOI's sanitation rules (TSI above the threshold, personal
-- accident lines, endorsements of the term, Clean accounts For Renewal, total loss claims), the
-- duplicate criteria, the CBG Motor automatic values of Annexes D and E, the term of a manually
-- created renewal account, and the checks Total loss claim and Potential duplicate account.

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('RNW_TSI_ABOVE_ROUTE', 'QUOTATION', 'STRING', 'RENEWAL',
     'Total sum insured above the threshold: QUOTATION (Clean with For Quotation, to the TSU) or REVIEW (Review with a proposal for the TSU)',
     null, null, now(), 'SYSTEM'),
    ('RNW_PROPOSAL_LINES', 'PERSONAL_ACCIDENT', 'CODE_LIST', 'RENEWAL',
     'Product lines whose Clean renewal accounts are disposed For Proposal (personal accident and group personal accident)',
     null, null, now(), 'SYSTEM'),
    ('RNW_CLEAN_AUTO_RENEWAL', 'true', 'BOOLEAN', 'RENEWAL',
     'Clean renewal accounts are disposed For Renewal automatically and posted (true), or wait for the disposition of Marketing (false)',
     null, null, now(), 'SYSTEM'),
    ('RNW_ENDORSEMENT_ROUTE', 'ANY', 'STRING', 'RENEWAL',
     'Endorsements of the expiring term: ANY (every endorsement routes the renewal account to Review) or IN_PROGRESS (only an endorsement in progress)',
     null, null, now(), 'SYSTEM'),
    ('RNW_TOTAL_LOSS_SETTLEMENT_TYPES', 'SETTLED_TOTAL_LOSS', 'CODE_LIST', 'RENEWAL',
     'Claim settlement types of a total loss: the renewal account is Not for Renewal with the reason Total Loss Claim',
     null, null, now(), 'SYSTEM'),
    ('RNW_DUPLICATE_CRITERIA', '*:EXPIRING_POLICY+PN', 'STRING', 'RENEWAL',
     'Duplicate criteria per product line (LINE:CRITERIA+CRITERIA; * for every line) among CLIENT, RISK_CODE, PN and EXPIRING_POLICY',
     null, null, now(), 'SYSTEM'),
    ('RNW_DUPLICATE_MIN_MATCH', '1', 'INTEGER', 'RENEWAL',
     'Number of the duplicate criteria that must match for a potential duplicate (all of them is an exact duplicate)',
     1, 4, now(), 'SYSTEM'),
    ('RNW_CBG_MOTOR_AUTO_UPDATE', 'true', 'BOOLEAN', 'RENEWAL',
     'CBG Motor renewal accounts take the automatic values: depreciated sum insured, BI and PD limits stepped down, Auto Promo renewed as Regular',
     null, null, now(), 'SYSTEM'),
    ('RNW_CBG_MOTOR_BI_TIERS', '50000:115.00,100000:240.00,150000:310.00,200000:375.00,250000:430.00,300000:495.00,500000:650.00,750000:890.00,1000000:1100.00',
     'STRING', 'RENEWAL', 'Bodily injury limits and their premiums (limit:premium) for the step-down of a CBG Motor renewal',
     null, null, now(), 'SYSTEM'),
    ('RNW_CBG_MOTOR_PD_TIERS', '50000:150.00,100000:300.00,150000:400.00,200000:480.00,250000:520.00,300000:560.00,500000:720.00,750000:950.00,1000000:1150.00',
     'STRING', 'RENEWAL', 'Property damage limits and their premiums (limit:premium) for the step-down of a CBG Motor renewal',
     null, null, now(), 'SYSTEM'),
    ('RNW_AUTO_PROMO_RISK_CODES', 'MTR70,MTR71,MTR72,MTR73', 'CODE_LIST', 'RENEWAL',
     'Risk codes of the Auto Promo (first year free) motor policies', null, null, now(), 'SYSTEM'),
    ('RNW_PROMO_TO_REGULAR', 'true', 'BOOLEAN', 'RENEWAL',
     'An Auto Promo motor policy renews as a Regular policy with the risk code and rate of the maps', null, null, now(), 'SYSTEM'),
    ('RNW_PROMO_RISK_CODE_MAP', 'MTR70=MTR22,MTR71=MTR23,MTR72=MTR25,MTR73=MTR73', 'STRING', 'RENEWAL',
     'Regular risk code of each Auto Promo risk code', null, null, now(), 'SYSTEM'),
    ('RNW_PROMO_RATE_MAP', '2.4230=2.3760,1.9450=1.6060,2.0400=1.9630,3.0000=3.0000', 'STRING', 'RENEWAL',
     'Regular premium rate (percent) of each Auto Promo premium rate', null, null, now(), 'SYSTEM'),
    ('RNW_CTPL_RISK_CODES', 'CTP01', 'CODE_LIST', 'RENEWAL',
     'Risk codes of the compulsory third party liability policies (renewed at their expiring values)', null, null, now(), 'SYSTEM'),
    ('RNW_MANUAL_INCEPTION_OFFSET_DAYS', '1', 'INTEGER', 'RENEWAL',
     'A renewal account created by hand starts this number of days after the expiry of the expiring account and runs one year',
     0, 31, now(), 'SYSTEM')
on conflict (param_key) do nothing;

insert into rnw_check_setting (check_code, active, severity, parameters, record_status, authorized_by,
    authorized_at, created_at, created_by)
values ('TOTAL_LOSS', true, 'SYSTEM', null, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'),
       ('DUPLICATE_ACCOUNT', true, 'FAIL_REVIEW', null, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM')
on conflict (check_code) do nothing;

-- The settlement type that tags a total loss claim (closes the claim with a payment).
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'BCL_SETTLEMENT_TYPE', 'SETTLED_TOTAL_LOSS', 'Settled - Total Loss', 95, null, date '2020-01-01',
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
where exists (select 1 from lov_type t where t.code = 'BCL_SETTLEMENT_TYPE')
  and not exists (select 1 from lov_value v where v.type_code = 'BCL_SETTLEMENT_TYPE'
                  and v.code = 'SETTLED_TOTAL_LOSS');

insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select 'BCL_SETTLEMENT_TYPE', 'SETTLED_TOTAL_LOSS', a.attribute, a.value, now(), 'SYSTEM'
from (values ('outcome', 'SETTLED'), ('closes_claim', 'true'), ('requires_settlement_amount', 'true'))
     as a(attribute, value)
where not exists (select 1 from bcl_lov_attribute x where x.type_code = 'BCL_SETTLEMENT_TYPE'
                  and x.code = 'SETTLED_TOTAL_LOSS' and x.attribute = a.attribute);

-- A run of its own records each renewal account created by hand.
alter table rnw_extraction_run drop constraint ck_rnw_extraction_trigger;
alter table rnw_extraction_run add constraint ck_rnw_extraction_trigger
    check (trigger_kind in ('SCHEDULED', 'MANUAL_RANGE', 'UPLOAD', 'LEGACY', 'GOLIVE', 'MANUAL_ACCOUNT'));
