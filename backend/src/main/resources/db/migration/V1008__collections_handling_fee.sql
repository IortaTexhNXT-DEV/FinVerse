-- =====================================================================================
-- iNXT BrokerVerse - V1008 Collections and Cashiering: the Handling Fee disposition of unapplied
-- payments (contract change of Submitted Policies wave S0, in the Collections range; V1007 is kept
-- for Data Migration).
--   Requirements: docs/requirements/BDOI_SP_BRD_SPEC.md BRIDSP-31.
--   Design: docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 3.6 and 9;
--           docs/architecture/COLLECTIONS_DESIGN.md (UPP dispositions).
--   * The collector disposition HANDLING_FEE of CLX_UPP_DISPOSITION asks Cashiering to recognise
--     the payment as handling-fee income (action RECOGNIZE_INCOME, no invoice); the automatic tagger
--     of Submitted Policies sends the same request.
--   * The action checks of the collector requests (csh_collector_request, V1006) and of the
--     Collections tables (V1004) accept RECOGNIZE_INCOME.
-- =====================================================================================

alter table csh_collector_request drop constraint if exists ck_csh_collector_request_action;
alter table csh_collector_request add constraint ck_csh_collector_request_action
    check (action in ('APPLY_TO_INVOICE', 'REFUND', 'RECLASS', 'TRANSFER', 'RECOGNIZE_INCOME'));

alter table clx_application_request drop constraint if exists ck_clx_application_request_action;
alter table clx_application_request add constraint ck_clx_application_request_action
    check (action in ('APPLY_TO_INVOICE', 'REFUND', 'RECLASS', 'TRANSFER', 'RECOGNIZE_INCOME'));

alter table clx_unapplied_disposition drop constraint if exists ck_clx_unapplied_disposition_action;
alter table clx_unapplied_disposition add constraint ck_clx_unapplied_disposition_action
    check (cashiering_action in ('APPLY_TO_INVOICE', 'REFUND', 'RECLASS', 'TRANSFER', 'RECOGNIZE_INCOME', 'NONE'));

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
values ('CLX_UPP_DISPOSITION', 'HANDLING_FEE', 'Handling fee', 45, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
        now(), now(), 'SYSTEM')
on conflict (type_code, code) do nothing;

insert into clx_lov_attribute (type_code, code, attribute, value, created_at, created_by)
values ('CLX_UPP_DISPOSITION', 'HANDLING_FEE', 'requires_invoice', 'false', now(), 'SYSTEM'),
       ('CLX_UPP_DISPOSITION', 'HANDLING_FEE', 'cashiering_action', 'RECOGNIZE_INCOME', now(), 'SYSTEM')
on conflict (type_code, code, attribute) do nothing;
