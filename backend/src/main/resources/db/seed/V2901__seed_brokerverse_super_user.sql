-- Super user of the DEV, SIT and UAT environments: Brokerverse (iorta TechNXT Super User). SEED DATA
-- ONLY - never load in production (the start-up safeguards refuse the seed profile there).
--
-- The group profile BDOI_SUPER_USER holds every permission; the user also holds the platform roles
-- of the system administration and of User Access (System Administrator, User Access Administrator,
-- Group Profile Maker and Checker, Second Approver, Information Security), so that every menu and
-- every maker, checker and approver stage opens for it. The seed runner SeedSuperUser keeps the
-- profile complete when permissions are added later and gives the user its password from
-- BROKERVERSE_SEED_SUPER_USER_PASSWORD; the script itself carries the shared hash of the seed users.

insert into sec_role (code, name, description, privilege_level, created_at, created_by)
select 'BDOI_SUPER_USER', 'Super User (DEV / SIT / UAT)',
       'Every permission of the system; for the implementation team in DEV, SIT and UAT only',
       'ADMIN', now(), 'SYSTEM'
where not exists (select 1 from sec_role where code = 'BDOI_SUPER_USER');

insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values
    ('ACCESS_APPROVE'), ('ACCESS_REQUEST'), ('ACCOUNTING_RULE_MANAGE'), ('ACCOUNT_MAINTAIN'),
    ('ACCOUNT_PROCESS'), ('ACCOUNT_VIEW'), ('ACSL_APPLY'), ('ACSL_APPROVE'), ('ACSL_ASSIGN'),
    ('ACSL_PROCESS'), ('ACSL_REPORT_EXPORT'), ('ACSL_REPORT_VIEW'), ('ACSL_REVIEW'),
    ('ACSL_UPLOAD'), ('ACSL_VIEW'), ('ADJ_APPROVE'), ('ADJ_POST'), ('ADJ_PROCESS'),
    ('ADJ_REQUEST'), ('ALERT_MANAGE'), ('ALERT_VIEW'), ('ASSET_MANAGE'), ('ATTACHMENT_MANAGE'),
    ('ATTACHMENT_VIEW'), ('AUDIT_VIEW'), ('BCL_ACTION_PLAN'), ('BCL_ADJUSTER_ASSIGN'),
    ('BCL_AUTHORIZE'), ('BCL_CLAIMANT_OVERRIDE'), ('BCL_CLOSE'), ('BCL_COVER_VIEW'),
    ('BCL_DATA_EXTRACT'), ('BCL_FOLLOW_UP_OVERRIDE'), ('BCL_LOCATION_REF_MAINTAIN'),
    ('BCL_RECORD'), ('BCL_REOPEN'), ('BCL_REPORT_EXPORT'), ('BCL_REPORT_VIEW'),
    ('BCL_RESERVE_AMEND'), ('BCL_SETTLEMENT_UPDATE'), ('BCL_SETUP'), ('BCL_STATUS_UPDATE'),
    ('BCL_VIEW'), ('BILLING_MANAGE'), ('BIR_CERT_ACK'), ('BIR_CERT_SUBMIT'), ('BOOKING_ADJUST'),
    ('BOOKING_PROCESS'), ('BUDGET_MANAGE'), ('BULK_PROCESS'), ('CASH_APPLY'), ('CASH_APPROVE'),
    ('CASH_CANCEL'), ('CASH_DISPOSITION'), ('CASH_DISPOSITION_APPROVE'), ('CASH_PRINT'),
    ('CASH_RECEIPT'), ('CASH_REINSTATE'), ('CASH_SERIES_MANAGE'), ('CASH_UPLOAD'),
    ('CASH_UPP_INCOME_APPROVE'), ('CASH_UPP_INCOME_REQUEST'), ('CLIENT_APPROVE'),
    ('CLIENT_MAINTAIN'), ('CLIENT_VIEW'), ('CLX_ASSIGN'), ('CLX_AUDIT_VIEW'), ('CLX_BILLING'),
    ('CLX_BULK_UPDATE'), ('CLX_ESCALATE'), ('CLX_ESCALATION_HANDLE'), ('CLX_EXPORT'),
    ('CLX_REPORT_VIEW'), ('CLX_SETUP'), ('CLX_SOA_ISSUE'), ('CLX_UNAPPLIED_WORK'), ('CLX_VIEW'),
    ('CLX_WORK'), ('COA_UPLOAD'), ('COMMREC_APPROVE'), ('COMMREC_PROCESS'),
    ('CONFIG_BASELINE_MANAGE'), ('CONFIG_EXPORT'), ('CONFIG_IMPORT_APPROVE'),
    ('CONFIG_IMPORT_PREPARE'), ('CSF_CONTACT_UPDATE'), ('CSF_DOCUMENT_UPLOAD'),
    ('CSF_REPORT_VIEW'), ('CSF_RESEND'), ('CSF_RESEND_OTHER'), ('CSF_VIEW'), ('CWT_PROCESS'),
    ('CWT_TAG'), ('DASHBOARD_VIEW'), ('DISB_APPROVE'), ('DISB_EOD'), ('DISB_FUNDING_APPROVE'),
    ('DISB_FUNDING_REQUEST'), ('DISB_FUNDING_VERIFY'), ('DISB_PAYEE_AUTHORIZE'),
    ('DISB_PAYEE_MAINTAIN'), ('DISB_PAYEE_VIEW_FULL'), ('DISB_PROCESS'), ('DISB_REPORT_EXPORT'),
    ('DISB_REPORT_VIEW'), ('DISB_REVIEW'), ('DISB_STATUS_APPROVE'), ('DISB_TAG'), ('DISB_UPLOAD'),
    ('DISB_VIEW'), ('EB_COLLECT'), ('EB_COMPARATIVE_APPROVE'), ('EB_MARKET'), ('EB_PROCESS'),
    ('EB_REPORT_VIEW'), ('EB_SETUP'), ('EB_THRESHOLD_APPROVE'), ('EB_VIEW'), ('EMPLOYEE_MAINTAIN'),
    ('EPOLICY_MANAGE'), ('EPOLICY_SEND'), ('FILE_LEGAL_HOLD_APPROVE'), ('FILE_LEGAL_HOLD_REQUEST'),
    ('FILE_QUARANTINE_VIEW'), ('FLOWIN_MANAGE'), ('FRBS_REPORT_EXPORT'), ('FRBS_REPORT_VIEW'),
    ('GL_CLOSE_SCHEDULE'), ('HOLD_APPROVE'), ('HOLD_REQUEST'), ('IAAF_APPROVE'), ('IAAF_PREPARE'),
    ('INCENTIVE_CRITERIA_MAINTAIN'), ('INCENTIVE_MANAGE'), ('INVESTMENT_MANAGE'),
    ('JOURNAL_ASSIGN'), ('JOURNAL_AUTHORIZE'), ('JOURNAL_CREATE'), ('JOURNAL_REVERSE'),
    ('JOURNAL_VIEW'), ('LEGACY_ACCESS_LOG_VIEW'), ('LEGACY_INQUIRY_EXPORT'),
    ('LEGACY_INQUIRY_VIEW'), ('LEGACY_REVERSAL_APPROVE'), ('LEGACY_REVERSAL_REQUEST'),
    ('LOV_MANAGE'), ('MASTER_AUTHORIZE'), ('MASTER_MAINTAIN'), ('MASTER_VIEW'), ('MESSAGE_VIEW'),
    ('METRICS_VIEW'), ('MIG_CUTOVER_MANAGE'), ('MIG_DECISION_APPROVE'), ('MIG_DQ_RESOLVE'),
    ('MIG_DQ_WAIVE'), ('MIG_GONOGO_DECIDE'), ('MIG_INTAKE'), ('MIG_LOAD_APPROVE'),
    ('MIG_LOAD_RUN'), ('MIG_MAPPING_APPROVE'), ('MIG_MAPPING_EDIT'), ('MIG_MATCH_DECIDE'),
    ('MIG_OBJECT_MANAGE'), ('MIG_RECON_SIGNOFF'), ('MIG_RESUBMIT_APPROVE'),
    ('MIG_ROLLBACK_APPROVE'), ('MIG_ROLLBACK_REQUEST'), ('MIG_SIGNOFF'), ('MIG_TRUEUP_APPROVE'),
    ('MIG_TRUEUP_PREPARE'), ('MIG_VIEW'), ('MODULE_SWITCH_APPROVE'), ('MODULE_SWITCH_MANAGE'),
    ('OPS_REPORT_EXPORT'), ('OPS_REPORT_VIEW'), ('OPS_VIEW'), ('PERIOD_END_RUN'),
    ('PERIOD_MANAGE'), ('PKG_ADVISORY'), ('PKG_MANCOM_SIGNOFF'), ('PKG_NEGOTIATE'),
    ('PKG_QS_APPROVE'), ('PKG_REPORT_VIEW'), ('PKG_REQUEST'), ('PKG_REQUEST_APPROVE'),
    ('PKG_TSU_APPROVE'), ('PKG_TSU_RECOMMEND'), ('PLACEMENT_MANAGE'), ('PRODUCT_ARCHIVE_VIEW'),
    ('PRODUCT_AUTHORIZE'), ('PRODUCT_MAINTAIN'), ('PRODUCT_VALIDATE'), ('PRODUCT_VIEW'),
    ('PROPOSAL_APPROVE'), ('PROPOSAL_REQUEST'), ('PRQ_APPROVE'), ('PRQ_ASSIGN'), ('PRQ_CREATE'),
    ('PRQ_HR_APPROVE'), ('PRQ_REVIEW'), ('PRQ_VIEW'), ('QUOTE_APPROVE'), ('QUOTE_MAINTAIN'),
    ('QUOTE_VIEW'), ('RECEIPT_PAYMENT_AUTHORIZE'), ('RECEIPT_PAYMENT_MAINTAIN'),
    ('RECONCILIATION_MANAGE'), ('RECON_PROCESS'), ('RECON_SEND'), ('REMIT_APPROVE'),
    ('REMIT_DEDUCTION_CONFIRM'), ('REMIT_EXCLUDE'), ('REMIT_EXTRACT'), ('REMIT_OR_UPLOAD'),
    ('REMIT_PROCESS'), ('REPORT_FINANCIAL'), ('REPORT_VIEW'), ('REVALUATION_RATE_MAINTAIN'),
    ('RNW_ACCEPT'), ('RNW_ASSIGN'), ('RNW_AUDIT_VIEW'), ('RNW_BILLING'), ('RNW_BUDGET'),
    ('RNW_CHANNEL_MONITOR'), ('RNW_CHANNEL_SETUP'), ('RNW_DISPOSE'), ('RNW_EXPORT'),
    ('RNW_EXTRACT'), ('RNW_FOLLOWUP'), ('RNW_INSURER'), ('RNW_KYC'), ('RNW_LAMD_UPLOAD'),
    ('RNW_OVERRIDE'), ('RNW_PACKAGE_REMAP'), ('RNW_PROCESS'), ('RNW_PROCESS_ASSIGN'),
    ('RNW_RA_GENERATE'), ('RNW_RA_SEND'), ('RNW_REPORT_VIEW'), ('RNW_REVIEW'),
    ('RNW_RMU_MAINTAIN'), ('RNW_SETUP'), ('RNW_TEMPLATE_MAINTAIN'), ('RNW_TSU_QUEUE'),
    ('RNW_UPLOAD'), ('RNW_VALIDATE'), ('RNW_VIEW'), ('ROLE_HOME_APPROVE'), ('ROLE_HOME_MANAGE'),
    ('ROLE_MANAGE'), ('SBM_EXPORT'), ('SBM_HANDLING_FEE'), ('SBM_INTAKE'), ('SBM_LETTER_SEND'),
    ('SBM_MAINTAIN'), ('SBM_MIGRATE'), ('SBM_PROCESS'), ('SBM_PROPOSAL'), ('SBM_REPORT_EXPORT'),
    ('SBM_REPORT_VIEW'), ('SBM_RULE_APPROVE'), ('SBM_RULE_MAINTAIN'), ('SBM_VIEW'),
    ('SCR_AUDIT_VIEW'), ('SCR_CASE_APPROVE'), ('SCR_CASE_ASSIGN'), ('SCR_COMMITTEE'),
    ('SCR_COMPLIANCE_REVIEW'), ('SCR_CONFIG_APPROVE'), ('SCR_CONFIG_MAINTAIN'),
    ('SCR_INVESTIGATE'), ('SCR_LIST_APPROVE'), ('SCR_LIST_MAINTAIN'), ('SCR_REPORT_VIEW'),
    ('SCR_RISK_TAG'), ('SCR_STR_EXTRACT'), ('SCR_VIEW'), ('SECURITY_PARAMETER_APPROVE'),
    ('SERVICE_FEE_APPROVE'), ('SERVICE_FEE_MANAGE'), ('SERVICE_FEE_TAG'),
    ('SPECIAL_REMIT_APPROVE'), ('SPECIAL_REMIT_REQUEST'), ('SYSTEM_MONITOR'),
    ('SYSTEM_PARAMETER_MANAGE'), ('TAX_MANAGE'), ('TAX_VIEW'), ('TOR_APPROVE'), ('TOR_PREPARE'),
    ('TSU_APPROVE'), ('TSU_PROCESS'), ('UAM_CANCEL'), ('UAM_GROUP_REQUEST'), ('UAM_REPORT_VIEW'),
    ('UAM_SECOND_APPROVE'), ('UAM_SOD_AUTHORIZE'), ('UAM_SOD_MAINTAIN'), ('UAM_VIEW'),
    ('USER_MANAGE'), ('USER_VIEW'), ('WORK_ASSIGN'), ('WORK_VIEW'), ('YEAR_END_CLOSE')
) as p(permission)
where r.code = 'BDOI_SUPER_USER'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at, created_by)
select 'Brokerverse', 'iorta TechNXT Super User', 'brokerverse@uat.brokerverse.cloud',
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
where not exists (select 1 from sec_user x where lower(x.username) = 'brokerverse');

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join sec_role r on r.code in ('BDOI_SUPER_USER', 'SYSADMIN', 'UAM_ADMINISTRATOR', 'UAM_REQUESTOR',
                              'UAM_APPROVER', 'UAM_SECOND_APPROVER', 'INFOSEC_OFFICER')
where lower(u.username) = 'brokerverse'
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
