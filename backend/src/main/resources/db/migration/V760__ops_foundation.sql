-- =====================================================================================
-- iNXT BrokerVerse - V760 Operations (BRD-2) foundation: roles and grants, lists of values,
-- business parameters and exception codes shared by every Operations module.
--   Requirements: docs/requirements/BDOI_OPS_BRD_SPEC.md
--     BRQID.001/002  access restricted to authorised users; Operations roles (OQ48: final matrix)
--     BRQID.003      Operations home, external application links (LOV OPS_EXTERNAL_LINK)
--     CSHID.002/003/004/024, RMTID.001/020/029, MKTID.003/009, PRCID.016/038/039,
--     ADJID.002/004/025, CMRID.003/008  coded fields as lists of values
--   Design: docs/architecture/OPERATIONS_DESIGN.md sections 6 (security) and 10 (parameters,
--   alerts, LOV types). The permissions are the enum security.domain.Permission.
--   Lists given by the BRD are seeded as written; lists the BRD leaves open hold only
--   'Others' (or nothing) until BDOI supplies them (OQ31, OQ32, OQ40, OQ45).
-- =====================================================================================

-- ---------- Operations role profiles (OPERATIONS_DESIGN 6.2) ----------------------------
insert into sec_role (code, name, created_at, created_by)
values ('CASHIER', 'Cashiering (HO / branch)', now(), 'SYSTEM'),
       ('CASHIER_TL', 'Cashiering Team Leader / Head', now(), 'SYSTEM'),
       ('REMIT_PROCESSOR', 'Remittance Processor', now(), 'SYSTEM'),
       ('REMIT_TL', 'Remittance Team Leader / Head', now(), 'SYSTEM'),
       ('RECON_HANDLER', 'Production Reconciliation Handler', now(), 'SYSTEM'),
       ('ADJUSTMENT_TL', 'Adjustment Team Leader', now(), 'SYSTEM'),
       ('COMMREC_HANDLER', 'Commission Receivables Handler', now(), 'SYSTEM'),
       ('COMMREC_TL', 'Commission Receivables Team Leader / Head', now(), 'SYSTEM'),
       ('MKT_COLLECTION', 'Marketing Collection (HO / branches)', now(), 'SYSTEM'),
       ('COMPTROLLERSHIP', 'Comptrollership', now(), 'SYSTEM'),
       ('DISBURSEMENT', 'Disbursement', now(), 'SYSTEM');

-- Every Operations role: Operations home and invoice 360, My Work, report catalogue and
-- Operations report viewing, documents, client and account look-ups.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('OPS_VIEW'), ('WORK_VIEW'), ('OPS_REPORT_VIEW'), ('REPORT_VIEW'),
                   ('ATTACHMENT_VIEW'), ('CLIENT_VIEW'), ('ACCOUNT_VIEW')) as p(permission)
where r.code in ('CASHIER', 'CASHIER_TL', 'REMIT_PROCESSOR', 'REMIT_TL', 'RECON_HANDLER',
                 'ADJUSTMENT', 'ADJUSTMENT_TL', 'COMMREC_HANDLER', 'COMMREC_TL', 'MKT_COLLECTION',
                 'MKT_TL', 'COMPTROLLERSHIP', 'DISBURSEMENT')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Handlers, team leaders and heads: report export, uploads and document maintenance.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('OPS_REPORT_EXPORT'), ('BULK_PROCESS'), ('ATTACHMENT_MANAGE')) as p(permission)
where r.code in ('CASHIER', 'CASHIER_TL', 'REMIT_PROCESSOR', 'REMIT_TL', 'RECON_HANDLER',
                 'ADJUSTMENT', 'ADJUSTMENT_TL', 'COMMREC_HANDLER', 'COMMREC_TL')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (OPERATIONS_DESIGN 6.2).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Cashiering
    ('CASHIER', 'CASH_RECEIPT'), ('CASHIER', 'CASH_CANCEL'), ('CASHIER', 'CASH_REINSTATE'),
    ('CASHIER', 'CASH_APPLY'), ('CASHIER', 'CASH_UPLOAD'), ('CASHIER', 'CASH_DISPOSITION'),
    ('CASHIER', 'CASH_PRINT'), ('CASHIER', 'CWT_PROCESS'),
    ('CASHIER_TL', 'CASH_RECEIPT'), ('CASHIER_TL', 'CASH_CANCEL'), ('CASHIER_TL', 'CASH_REINSTATE'),
    ('CASHIER_TL', 'CASH_APPLY'), ('CASHIER_TL', 'CASH_UPLOAD'), ('CASHIER_TL', 'CASH_DISPOSITION'),
    ('CASHIER_TL', 'CASH_PRINT'), ('CASHIER_TL', 'CWT_PROCESS'), ('CASHIER_TL', 'CASH_APPROVE'),
    ('CASHIER_TL', 'CASH_DISPOSITION_APPROVE'), ('CASHIER_TL', 'CASH_SERIES_MANAGE'),
    ('CASHIER_TL', 'MASTER_VIEW'), ('CASHIER_TL', 'WORK_ASSIGN'),
    -- Remittance
    ('REMIT_PROCESSOR', 'REMIT_EXTRACT'), ('REMIT_PROCESSOR', 'REMIT_PROCESS'),
    ('REMIT_PROCESSOR', 'REMIT_EXCLUDE'), ('REMIT_PROCESSOR', 'REMIT_OR_UPLOAD'),
    ('REMIT_TL', 'REMIT_EXTRACT'), ('REMIT_TL', 'REMIT_PROCESS'), ('REMIT_TL', 'REMIT_EXCLUDE'),
    ('REMIT_TL', 'REMIT_OR_UPLOAD'), ('REMIT_TL', 'REMIT_APPROVE'),
    ('REMIT_TL', 'SPECIAL_REMIT_APPROVE'), ('REMIT_TL', 'WORK_ASSIGN'),
    -- Production reconciliation
    ('RECON_HANDLER', 'RECON_PROCESS'), ('RECON_HANDLER', 'RECON_SEND'),
    -- Adjustment (the ADJUSTMENT role exists since V750 with BOOKING_ADJUST)
    ('ADJUSTMENT', 'ADJ_PROCESS'), ('ADJUSTMENT', 'ADJ_POST'),
    ('ADJUSTMENT_TL', 'ADJ_PROCESS'), ('ADJUSTMENT_TL', 'ADJ_POST'), ('ADJUSTMENT_TL', 'ADJ_APPROVE'),
    ('ADJUSTMENT_TL', 'BOOKING_ADJUST'), ('ADJUSTMENT_TL', 'WORK_ASSIGN'),
    -- Commission receivables (direct payment) and incentives
    ('COMMREC_HANDLER', 'COMMREC_PROCESS'), ('COMMREC_HANDLER', 'BIR_CERT_SUBMIT'),
    ('COMMREC_TL', 'COMMREC_PROCESS'), ('COMMREC_TL', 'BIR_CERT_SUBMIT'),
    ('COMMREC_TL', 'COMMREC_APPROVE'), ('COMMREC_TL', 'INCENTIVE_MANAGE'), ('COMMREC_TL', 'WORK_ASSIGN'),
    -- Marketing Collection and Marketing TL / UH
    ('MKT_COLLECTION', 'CWT_TAG'), ('MKT_COLLECTION', 'HOLD_REQUEST'),
    ('MKT_COLLECTION', 'SPECIAL_REMIT_REQUEST'), ('MKT_COLLECTION', 'ADJ_REQUEST'),
    ('MKT_COLLECTION', 'BULK_PROCESS'), ('MKT_COLLECTION', 'ATTACHMENT_MANAGE'),
    ('MKT_TL', 'CWT_TAG'), ('MKT_TL', 'HOLD_REQUEST'), ('MKT_TL', 'HOLD_APPROVE'),
    ('MKT_TL', 'SPECIAL_REMIT_REQUEST'), ('MKT_TL', 'ADJ_REQUEST'),
    -- Comptrollership (accounting rules stay with the finance roles)
    ('COMPTROLLERSHIP', 'BIR_CERT_ACK'), ('COMPTROLLERSHIP', 'JOURNAL_VIEW'),
    -- Disbursement (default in-app queue until the Disbursement system is known, OQ02)
    ('DISBURSEMENT', 'DISB_PROCESS'),
    -- Existing roles: read access, interface administration and management reporting
    ('MKT_AO', 'OPS_VIEW'), ('PROCESSOR', 'OPS_VIEW'), ('PROCESSING_TL', 'OPS_VIEW'),
    ('BUSINESS_ADMIN', 'OPS_VIEW'),
    ('SYSADMIN', 'OPS_VIEW'), ('SYSADMIN', 'FLOWIN_MANAGE'), ('SYSADMIN', 'OPS_REPORT_VIEW'),
    ('FIN_MANAGER', 'OPS_VIEW'), ('FIN_MANAGER', 'OPS_REPORT_VIEW'), ('FIN_MANAGER', 'OPS_REPORT_EXPORT'),
    ('AUDITOR', 'OPS_VIEW'), ('AUDITOR', 'OPS_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Lists of values (OPERATIONS_DESIGN section 10) ------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('OR_TYPE', 'Official receipt type', 'BIR official receipt types of BDOI income (CSHID.002)', true, now(), 'SYSTEM'),
       ('AR_CLASS', 'Acknowledgement receipt class', 'Premium and non-premium payment classes and channels of an AR (CSHID.001)', true, now(), 'SYSTEM'),
       ('RECEIPT_CANCEL_REASON', 'Receipt cancellation reason', 'Why an AR or OR is cancelled; parent = reason group (CSHID.003)', true, now(), 'SYSTEM'),
       ('REINSTATEMENT_REASON', 'Reinstatement reason', 'Why a receipt is reinstated; parent = PREMIUM or DIRECT_PAYMENT (CSHID.004)', true, now(), 'SYSTEM'),
       ('DISPOSITION_TYPE', 'Unapplied payment disposition', 'Disposition of an unapplied or excess payment (CSHID.024)', true, now(), 'SYSTEM'),
       ('REMITTANCE_TYPE', 'Remittance type', 'Remittance extraction type (RMTID.001)', true, now(), 'SYSTEM'),
       ('REMIT_EXCLUSION_REASON', 'Remittance exclusion reason', 'Why an invoice is excluded from a remittance batch (RMTID.002 addendum, RMTID.020)', true, now(), 'SYSTEM'),
       ('REMIT_RETURN_REASON', 'Remittance return reason', 'Why a remittance batch or line is returned (RMTID.029)', true, now(), 'SYSTEM'),
       ('HOLD_REASON', 'Remittance hold reason', 'Why Marketing holds an account from remittance (MKTID.003, OQ24)', true, now(), 'SYSTEM'),
       ('SPECIAL_REMIT_CONDITION', 'Special remittance condition', 'Condition of a special remittance request (MKTID.009)', true, now(), 'SYSTEM'),
       ('RECON_COMPANY_CONCERNED', 'Company concerned', 'Party concerned by a reconciliation discrepancy (PRCID.016, OQ31)', true, now(), 'SYSTEM'),
       ('RECON_DISPOSITION', 'Reconciliation disposition', 'Disposition of an unmatched or discrepant production line (PRCID.038/039, OQ31)', true, now(), 'SYSTEM'),
       ('ENDORSEMENT_TYPE', 'Endorsement type', 'Financial, non-financial and internal endorsement types; parent = class (ADJID.002/004)', true, now(), 'SYSTEM'),
       ('ENDORSEMENT_REQUEST_TYPE', 'Endorsement request type', 'Request types of the endorsement slip (Annex V)', true, now(), 'SYSTEM'),
       ('ENDORSEMENT_DOC_TYPE', 'Endorsement document type', 'Supporting document of an endorsement request (ADJID.025, OQ32)', true, now(), 'SYSTEM'),
       ('DP_FEEDBACK_REASON', 'Direct payment feedback reason', 'Insurer feedback on a direct payment commission billing (CMRID.008, OQ40)', true, now(), 'SYSTEM'),
       ('INCENTIVE_EXCLUSION_RULE', 'Incentive exclusion rule', 'Production excluded from incentive eligibility (CMRID.003, OQ39)', true, now(), 'SYSTEM'),
       ('OPS_EXTERNAL_LINK', 'Operations external link', 'Integrated applications shown on the Operations home; label "Name|https://url" (BRQID.003, OQ01)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, v.parent_code, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    -- OR types (CSHID.002)
    ('OR_TYPE', 'SERVICE_FEE', 'Service Fee Income (e.g. risk management fee, consultancy fee)', 10, null),
    ('OR_TYPE', 'PROFIT_SHARE', 'Insurance Profit Share', 20, null),
    ('OR_TYPE', 'COMMISSION', 'Commissions (with VAT / withholding tax)', 30, null),
    ('OR_TYPE', 'INCENTIVE', 'Incentives', 40, null),
    ('OR_TYPE', 'OTHERS', 'Others (discount)', 90, null),
    -- AR classes and channels (CSHID.001)
    ('AR_CLASS', 'PREMIUM', 'Premium payments', 10, null),
    ('AR_CLASS', 'BILLS_PAYMENT', 'Bills Payment', 11, 'PREMIUM'),
    ('AR_CLASS', 'OTC', 'OTC', 12, 'PREMIUM'),
    ('AR_CLASS', 'TRADE', 'Trade', 13, 'PREMIUM'),
    ('AR_CLASS', 'CLPC', 'CLPC', 14, 'PREMIUM'),
    ('AR_CLASS', 'PDC', 'PDC', 15, 'PREMIUM'),
    ('AR_CLASS', 'DIRECT_CREDIT', 'Direct Credit', 16, 'PREMIUM'),
    ('AR_CLASS', 'NON_PREMIUM', 'Non-premium payments (insurance payments)', 20, null),
    ('AR_CLASS', 'REFUND', 'Refund', 21, 'NON_PREMIUM'),
    ('AR_CLASS', 'OTHER_EXPENSES', 'Other expenses', 22, 'NON_PREMIUM'),
    ('AR_CLASS', 'AR_INSURANCE', 'AR Insurance', 23, 'NON_PREMIUM'),
    -- Receipt cancellation reasons by group (CSHID.003)
    ('RECEIPT_CANCEL_REASON', 'PRM_CHECK_AMOUNT', 'Discrepancy in check amount', 10, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'PRM_NO_SIGNATURE', 'No signature', 11, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'PRM_INCORRECT_PAYEE', 'Incorrect payee', 12, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'PRM_CHECK_NOT_PICKED_UP', 'Check not picked-up', 13, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'PRM_INCORRECT_CHECK', 'Incorrect check details provided by the client or Insurer', 14, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'PRM_OTHERS', 'Others (specify)', 19, 'PREMIUM'),
    ('RECEIPT_CANCEL_REASON', 'COM_INCORRECT_DETAILS', 'Incorrect payment details', 20, 'COMMISSION'),
    ('RECEIPT_CANCEL_REASON', 'COM_NEGATIVE_VAT', 'Negative VAT', 21, 'COMMISSION'),
    ('RECEIPT_CANCEL_REASON', 'COM_DISCOUNT_NOT_APPLIED', 'Discount not applied', 22, 'COMMISSION'),
    ('RECEIPT_CANCEL_REASON', 'PDC_REPLACEMENT', 'PDC retrieval / replacement - not yet due: for replacement', 30, 'PDC'),
    ('RECEIPT_CANCEL_REASON', 'PDC_PULL_OUT', 'PDC retrieval / replacement - not yet due: pull out', 31, 'PDC'),
    ('RECEIPT_CANCEL_REASON', 'GEN_BOUNCED_CHECK', 'Bounced or returned check (reinstatement needs no form)', 40, 'GENERAL'),
    ('RECEIPT_CANCEL_REASON', 'GEN_ISSUANCE_ERROR', 'Error in issuance details', 41, 'GENERAL'),
    ('RECEIPT_CANCEL_REASON', 'GEN_PRINTING_ERROR', 'Error printing', 42, 'GENERAL'),
    ('RECEIPT_CANCEL_REASON', 'GEN_CHECK_NOT_PICKED_UP', 'Check not picked up', 43, 'GENERAL'),
    ('RECEIPT_CANCEL_REASON', 'GEN_DOUBLE_ISSUANCE', 'Double issuance', 44, 'GENERAL'),
    ('RECEIPT_CANCEL_REASON', 'GEN_OTHERS', 'Others (specify)', 49, 'GENERAL'),
    -- Reinstatement reasons (CSHID.004)
    ('REINSTATEMENT_REASON', 'PRM_CANCELLATION', 'Due to cancellation', 10, 'PREMIUM'),
    ('REINSTATEMENT_REASON', 'PRM_MISAPPLICATION', 'Mis-application of payment', 11, 'PREMIUM'),
    ('REINSTATEMENT_REASON', 'PRM_WTAX_ADJUSTMENT', 'Withholding tax adjustment', 12, 'PREMIUM'),
    ('REINSTATEMENT_REASON', 'PRM_OTHERS', 'Others (specify)', 19, 'PREMIUM'),
    ('REINSTATEMENT_REASON', 'DP_DOUBLE_REVERSAL', 'Due to double reversal', 20, 'DIRECT_PAYMENT'),
    ('REINSTATEMENT_REASON', 'DP_WRONG_OR_DETAILS', 'Due to wrong details of OR', 21, 'DIRECT_PAYMENT'),
    ('REINSTATEMENT_REASON', 'DP_WTAX_ADJUSTMENT', 'Withholding tax adjustment', 22, 'DIRECT_PAYMENT'),
    ('REINSTATEMENT_REASON', 'DP_CANCELLATION', 'Due to cancellation', 23, 'DIRECT_PAYMENT'),
    ('REINSTATEMENT_REASON', 'DP_OTHERS', 'Others (specify)', 29, 'DIRECT_PAYMENT'),
    -- Unapplied payment dispositions (CSHID.024)
    ('DISPOSITION_TYPE', 'APPLY_OTHER_INVOICE', 'Apply to other invoice', 10, null),
    ('DISPOSITION_TYPE', 'DST_APPLICATION', 'DST payment application', 20, null),
    ('DISPOSITION_TYPE', 'REFUND', 'Refund', 30, null),
    ('DISPOSITION_TYPE', 'RECLASS', 'Reclass', 40, null),
    ('DISPOSITION_TYPE', 'TRANSFER_UNIT', 'Transfer to other marketing unit', 50, null),
    ('DISPOSITION_TYPE', 'OTHERS', 'Others (customizable)', 90, null),
    -- Remittance types (RMTID.001)
    ('REMITTANCE_TYPE', 'WITH_INCENTIVES', 'With Incentives', 10, null),
    ('REMITTANCE_TYPE', 'NORMAL_USD', 'Normal - Dollar', 20, null),
    ('REMITTANCE_TYPE', 'NORMAL_PHP', 'Normal - Peso', 30, null),
    -- Remittance exclusion reasons (RMTID.002/014/017/020)
    ('REMIT_EXCLUSION_REASON', 'ON_HOLD', 'Account on hold', 10, null),
    ('REMIT_EXCLUSION_REASON', 'PENDING_NEG_ADJ', 'Pending negative adjustment', 20, null),
    ('REMIT_EXCLUSION_REASON', 'WRITTEN_OFF', 'Written off', 30, null),
    ('REMIT_EXCLUSION_REASON', 'CHECK_HOLDING', 'Check within the holding period or not cleared', 40, null),
    ('REMIT_EXCLUSION_REASON', 'PAID_AR_OVER_DTIP', 'Paid AR greater than DTIP', 50, null),
    ('REMIT_EXCLUSION_REASON', 'NOT_POSTED', 'Payment not yet applied and posted', 60, null),
    ('REMIT_EXCLUSION_REASON', 'OTHERS', 'Others (see comment)', 90, null),
    -- Remittance return reasons (RMTID.029)
    ('REMIT_RETURN_REASON', 'OTHERS', 'Others (see comment)', 90, null),
    -- Hold reasons (MKTID.003; list to be supplied by BDOI, OQ24)
    ('HOLD_REASON', 'OTHERS', 'Others (see comment)', 90, null),
    -- Special remittance conditions (MKTID.009)
    ('SPECIAL_REMIT_CONDITION', 'CLAIMS', 'Claims', 10, null),
    ('SPECIAL_REMIT_CONDITION', 'RENEWAL', 'Renewal', 20, null),
    ('SPECIAL_REMIT_CONDITION', 'INSTALLMENT_DUE', 'Installment due', 30, null),
    ('SPECIAL_REMIT_CONDITION', 'IMMEDIATE_OR', 'Immediate OR issuance', 40, null),
    -- Endorsement types by class (ADJID.002/004)
    ('ENDORSEMENT_TYPE', 'FIN_TSI', 'Change of Total Sum Insured (increase or decrease)', 10, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_ITEMS', 'Change in insured items, persons / members, units, locations', 11, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_COMMISSION_RATE', 'Change in commission rate (increase or decrease)', 12, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_PREMIUM_RATE', 'Change in premium rate (increase or decrease) or premium amount', 13, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_EXTENSION', 'Extension of cover (period of cover)', 14, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_CHANGE_COVER', 'Change of cover', 15, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_CHARGES', 'Adjustment in charges (DST, premium tax / VAT, LGT, others) - increase or decrease', 16, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'FIN_MINIMAL_BALANCE', 'Minimal balance', 17, 'FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'NF_DESCRIPTIVE', 'Descriptive changes (assured, person, risk / item, perils, coverage, extensions, warranties, clauses, deductibles)', 20, 'NON_FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'NF_PERIOD_CHANGE', 'Change of period cover (inception moved earlier / later, term unchanged)', 21, 'NON_FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'NF_PERIOD_EXTENSION', 'Extension of period covered (inception and expiry moved, no financial impact)', 22, 'NON_FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'NF_COVER_EXTENSION', 'Extension of cover (extensions / clauses / warranties / deductibles added, no premium)', 23, 'NON_FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'NF_ASSURED_INFO', 'Change of assured name or information (address, birthdate, occupation)', 24, 'NON_FINANCIAL'),
    ('ENDORSEMENT_TYPE', 'INT_ADJUSTMENT', 'Internal adjustment (no insurer endorsement needed)', 30, 'INTERNAL'),
    -- Endorsement request types (Annex V)
    ('ENDORSEMENT_REQUEST_TYPE', 'FLAT_CANCELLATION', 'Flat Cancellation', 10, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'FLAT_CANCELLATION_RETAIN_DST', 'Flat Cancellation - Retain DST', 20, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'PARTIAL_CANCELLATION', 'Partial Cancellation', 30, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'TSI_CHANGE', 'Increase / Decrease in TSI', 40, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'PREMIUM_RATE_CHANGE', 'Increase / Decrease of Premium Rate', 50, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'VAT_PTAX_EXEMPT', 'VAT / Premium Tax exempt', 60, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'TAXES_CHANGE', 'Increase / Decrease of taxes (LGT, FST, other taxes)', 70, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'CHANGE_OF_COVER', 'Change of Cover', 80, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'EXTENSION_OF_COVER', 'Extension of Cover', 90, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'WRITE_OFF', 'Write-off', 100, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'COMMISSION_CHANGE', 'Decrease / Increase in Commission', 110, null),
    ('ENDORSEMENT_REQUEST_TYPE', 'CANCELLATION_REVERSAL', 'Cancellation Reversal', 120, null),
    -- Endorsement documents (list blank in the BRD, OQ32)
    ('ENDORSEMENT_DOC_TYPE', 'OTHERS', 'Other supporting document', 90, null),
    -- Direct payment feedback reasons (list to be supplied, OQ40)
    ('DP_FEEDBACK_REASON', 'OTHERS', 'Others (see comment)', 90, null),
    -- Incentive exclusions (CMRID.003)
    ('INCENTIVE_EXCLUSION_RULE', 'NEGATIVE_AMOUNT', 'Negative production amounts', 10, null),
    ('INCENTIVE_EXCLUSION_RULE', 'ERRONEOUS_BOOKING', 'Erroneous bookings', 20, null)
) as v(type_code, code, label, sort_order, parent_code);

-- Annex V cancellation reasons join the shared CANCELLATION_REASON list of V750 (codes kept).
insert into lov_value (type_code, code, label, sort_order, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'CANCELLATION_REASON', v.code, v.label, v.sort_order, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    ('DOLLAR_BOOKING', 'Dollar Booking', 100), ('BREACH_RATE', 'Breach Rate', 101),
    ('DELETION_EMPLOYEES', 'Deletion of employees', 102), ('DELETION_UNITS', 'Deletion of units', 103),
    ('DELETION_VEHICLE', 'Deletion of vehicle', 104), ('EXISTING_INSURANCE', 'With existing insurance', 105),
    ('OWN_POLICY', 'Client submitted own policy', 106), ('REPLACED_POLICY', 'Replaced by another policy', 107),
    ('NON_PAYMENT', 'Due to non-payment', 108), ('EGRESSED_TENANT', 'Egressed Tenant', 109),
    ('STORE_CLOSURE', 'Due to store closure', 110), ('DOUBLE_ISSUANCE', 'Double issuance', 111),
    ('DOUBLE_BOOKING', 'Double booking', 112), ('UNIT_SOLD', 'Unit sold', 113),
    ('UNIT_JUNKED', 'Unit junked', 114), ('UNIT_UNSERVICEABLE', 'Unit unserviceable', 115),
    ('UNIT_NOT_LOCATED', 'Unit cannot be located', 116), ('UNIT_REPOSSESSED', 'Unit repossessed', 117),
    ('UNIT_SURRENDERED', 'Unit surrendered', 118),
    ('NOT_MATERIALIZED', 'Project or transaction did not materialize', 119),
    ('LOAN_PAID_OTHER_UNIT', 'Loan fully paid with other BDO Units', 120),
    ('PREMIUM_CORRECTION', 'Correction of premium', 121),
    ('INCORRECT_POLICY_DETAILS', 'Due to incorrect policy details', 122),
    ('NOT_FOR_ISSUANCE', 'Not for issuance', 123), ('BOOKING_CORRECTION', 'To correct booking', 124),
    ('TRANSFERRED_RMU', 'Accounts transferred to RMU', 125),
    ('TRANSFERRED_OTHER_UNIT', 'Accounts transferred to other unit', 126),
    ('CASE_TERMINATED_RMU', 'Case terminated (RMU)', 127), ('CASE_DECIDED', 'Case decided', 128),
    ('CASE_DISMISSED', 'Case dismissed', 129),
    ('DIFFICULT_TO_COLLECT', 'Due to non-payment or difficult to collect', 130),
    ('INSURER_AUTO_CANCEL', 'Auto cancel by insurer due to nonpayment', 131),
    ('OTHERS', 'Others: Specify', 190)
) as v(code, label, sort_order)
on conflict (type_code, code) do nothing;

-- ---------- Business parameters (OPERATIONS_DESIGN section 10) ------------------------------
-- OPS_COMMISSION_REALIZATION is seeded by booking (V870).
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('REMIT_CHECK_HOLD_DAYS', '3', 'INTEGER', 'OPERATIONS',
     'Banking days a check must be held (and cleared) before its payment is remitted (RMTID.017)', 0, 30, now(), 'SYSTEM'),
    ('REMIT_PAIDAR_OVER_DTIP_MODE', 'EXCLUDE', 'STRING', 'OPERATIONS',
     'Paid AR greater than DTIP: EXCLUDE the invoice from extraction or CAP the remittance at the DTIP (RMTID.014, OQ19)', null, null, now(), 'SYSTEM'),
    ('RECON_TOLERANCE', '1.00', 'DECIMAL', 'OPERATIONS',
     'Production reconciliation tolerance: differences up to this amount are matched (PRCID.026)', 0, 1000, now(), 'SYSTEM'),
    ('MIN_BALANCE_AUTO_MAX', '10.00', 'DECIMAL', 'OPERATIONS',
     'Minimal balances up to this amount are reversed automatically (CSHID.016, OQ11)', 0, 1000, now(), 'SYSTEM'),
    ('MIN_BALANCE_FILE_RANGE', '10.00-100.00', 'STRING', 'OPERATIONS',
     'Balance range of the minimal balance write-off file (ADJID.026)', null, null, now(), 'SYSTEM'),
    ('CMR_FEEDBACK_WORKING_DAYS', '10', 'INTEGER', 'OPERATIONS',
     'Working days an insurer has to answer a direct payment commission billing (CMRID.011)', 1, 60, now(), 'SYSTEM'),
    ('CWT_APPLICATION_PERCENT', '98', 'DECIMAL', 'OPERATIONS',
     'Share of the premium applied for accounts of clients withholding 2% creditable tax (CSHID.020)', 50, 100, now(), 'SYSTEM'),
    ('PRODRECON_FILE_PATTERN', '', 'STRING', 'OPERATIONS',
     'File naming convention of the production register sent to insurers (PRCID.006, OQ29)', null, null, now(), 'SYSTEM'),
    ('REMIT_FILE_PATTERN', '', 'STRING', 'OPERATIONS',
     'File naming convention of the remittance extracts (RMTID.001, OQ18)', null, null, now(), 'SYSTEM'),
    ('OPS_BOOK_RATE_TYPE', 'BOOK', 'STRING', 'OPERATIONS',
     'Exchange rate type of Operations postings: the Comptrollership BOOK rate, 2 decimals (CSHID.012-014, OQ08)', null, null, now(), 'SYSTEM');

-- ---------- Exception codes (OPERATIONS_DESIGN section 10) ----------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('RECEIPT_SERIES_LOW', 'Receipt series running low',
     'An AR or OR series reached its warning threshold of remaining numbers (CSHID.015).',
     'CASHIERING', 'HIGH', null, null, now(), 'SYSTEM'),
    ('OPS_FLOW_IN_FAILED', 'Operations flow-in failed',
     'A run of an inbound or outbound Operations feed failed or had failed records (BRQID.005).',
     'OPSLEDGER', 'HIGH', null, null, now(), 'SYSTEM'),
    ('PREBOOKED_AGEING', 'Pre-booked payment ageing',
     'A payment matched to a pre-booked account is still waiting for the booking after the threshold days (CSHID.020).',
     'CASHIERING', 'MEDIUM', null, 5, now(), 'SYSTEM'),
    ('REMIT_EXTRACTION_FAILED', 'Remittance extraction failed',
     'A scheduled or manual remittance extraction failed (RMTID.001/005).',
     'REMITTANCE', 'HIGH', null, null, now(), 'SYSTEM'),
    ('REMIT_PAIDAR_OVER_DTIP', 'Paid AR greater than DTIP',
     'An invoice has more paid AR than the premium due to the insurer (RMTID.014, OQ19).',
     'REMITTANCE', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('DP_FEEDBACK_OVERDUE', 'Direct payment feedback overdue',
     'An insurer has not answered a direct payment commission billing within the working days allowed (CMRID.011).',
     'COMMISSION', 'MEDIUM', null, 10, now(), 'SYSTEM'),
    ('INCENTIVE_EXCLUSION', 'Production excluded from incentives',
     'Production lines were excluded from an incentive run by an exclusion rule (CMRID.003).',
     'COMMISSION', 'LOW', null, null, now(), 'SYSTEM'),
    ('RECON_UPLOAD_DUPLICATE', 'Duplicate insurer production upload',
     'An insurer production report identical to an earlier upload was refused (PRCID.010).',
     'PRODRECON', 'LOW', null, null, now(), 'SYSTEM'),
    ('ADJ_OVER_BASELINE', 'Adjustment over the original premium',
     'Cumulative adjustments of an invoice exceed its original premium or DTIP (ADJID.028).',
     'ADJUSTMENT', 'HIGH', null, null, now(), 'SYSTEM');
