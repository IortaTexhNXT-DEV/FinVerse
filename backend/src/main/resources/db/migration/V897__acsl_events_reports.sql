-- =====================================================================================
-- iNXT BrokerVerse - V897 ACSL notifications and parameters (ACSL 2.4.0, 2.5.4, 2.13.2).
-- ACSL corrections post as a system journal with the lines of the correction and have no event
-- type (ACCOUNTING_DISBURSEMENT_DESIGN 17); the reports are code (ReportMetadata.acsl, category ACSL).
-- =====================================================================================

insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('ACSL_CASE_STATUS', 'ACSL case or correction status', 'ACSL',
     'An ACSL case you requested has a result, or a correction entry you prepared was returned, approved or posted (ACSL 2.5.4, 2.10-2.12)',
     true, false, 310),
    ('ACSL_GLSL_DIFFERENCE', 'GL-SL difference found', 'ACSL',
     'The GL-SL reconciliation found a control account whose sub-ledger differs from the general ledger (ACSL 2.13.2)',
     true, true, 320);

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('ACSL_SOA_MAX_ROWS', '20000', 'INTEGER', 'ACSL',
     'Largest number of rows of one insurer statement of account upload (ACSL 2.4.0)', 1, 200000, now(), 'SYSTEM');
