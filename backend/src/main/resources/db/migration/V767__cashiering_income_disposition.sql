-- =====================================================================================
-- iNXT BrokerVerse - V767 Cashiering: income disposition of unapplied payments (contract change of
-- Submitted Policies wave S0, in the cashiering range V764-V769; V766 is kept for Data Migration).
--   Requirements: docs/requirements/BDOI_SP_BRD_SPEC.md BRIDSP-31 (handling-fee payments tagged in
--                 the Unapplied Payment List and applied with an official receipt).
--   Design: docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 3.6, 5 (rows 1-2) and 9.
--   * Disposition action INCOME: the payment leaves unapplied collections as BDOI income through
--     the accounting event of its type and an official receipt of the type's OR type is issued as
--     a no-cash settlement document.
--   * Disposition type HANDLING_FEE (no approval until BDOI decides otherwise), event
--     SBM_HANDLING_FEE (event type seeded by Submitted Policies in V1070), OR type HANDLING_FEE,
--     VAT included at 12 % (amount and VAT to confirm).
--   * csh_disposition.or_no keeps the official receipt of an income disposition.
--   The action of the collector requests (csh_collector_request, created by V1006) is widened in
--   V1008, which runs after that table exists.
-- =====================================================================================

alter table csh_disposition_type_rule drop constraint if exists csh_disposition_type_rule_action_check;
alter table csh_disposition_type_rule add constraint ck_csh_disposition_type_rule_action
    check (action in ('APPLY', 'DST_APPLY', 'REFUND', 'RECLASS', 'TRANSFER', 'MANUAL', 'INCOME'));
alter table csh_disposition_type_rule add column income_event varchar(40);
alter table csh_disposition_type_rule add column or_type varchar(40);
alter table csh_disposition_type_rule add column vat_rate numeric(9, 6) check (vat_rate is null or vat_rate >= 0);
alter table csh_disposition_type_rule add constraint ck_csh_disposition_type_rule_income
    check (action <> 'INCOME' or (income_event is not null and or_type is not null));

alter table csh_disposition add column or_no varchar(30);

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
values ('DISPOSITION_TYPE', 'HANDLING_FEE', 'Handling fee (submitted policy)', 60, null, date '2020-01-01',
        'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'),
       ('OR_TYPE', 'HANDLING_FEE', 'Handling Fee', 50, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(),
        'SYSTEM')
on conflict (type_code, code) do nothing;

insert into csh_disposition_type_rule (type_code, action, requires_approval, description, income_event, or_type,
    vat_rate)
values ('HANDLING_FEE', 'INCOME', false, 'Handling fee of a submitted policy recognised as income',
        'SBM_HANDLING_FEE', 'HANDLING_FEE', 0.120000);
