-- =====================================================================================
-- iNXT BrokerVerse - V1020 Claims Handling (BRD-7, broking claims) foundation: roles and grants,
-- permission action classes, lists of values and their attributes (bcl_lov_attribute), the status
-- access matrix (bcl_status_access), the claims handler register (bcl_handler), business
-- parameters, exception codes, notification events, workflow BCL_CLAIM, the loss advice template,
-- the retention rule of claims, the platform document type CLAIM_REPORT and the owner permission
-- of a list of values.
--   Requirements: docs/requirements/BDOI_CLM_BRD_SPEC.md
--     BRCLM.001        premium check and claims authorization code (BCL_AUTH_DP_POLICY, CLQ01)
--     BRCLM.002-006    role-restricted actions (stakeholder matrix p.28; OQ48, CLQ04, CLQ06)
--     BRCLM.009        default claim currency (BCL_DEFAULT_CURRENCY)
--     BRCLM.010-015    18 claim statuses and 10 settlement types with their attributes (spec 6.1-6.2)
--     BRCLM.012/013    status access matrix per role and unit (bcl_status_access; CLQ04)
--     BRCLM.017/018    adjusters / appraisers (spec 6.3; CLQ11)
--     BRCLM.019/022/034  follow-up days, follow-up and diary reminders (BCL_FOLLOW_UP_DAYS)
--     BRCLM.025-028/031  ageing buckets and past due threshold (BCL_AGEING_BUCKETS, BCL_PAST_DUE_DAYS)
--     BRCLM.035        temporary / permanent closure and reopen (workflow BCL_CLAIM, CLQ06)
--     BRCLM.036        catastrophe codes (spec 6.4)
--     BRCLM.038        claims-prone locations (BCL_PRONE_MIN_CLAIMS, BCL_PRONE_YEARS; CLQ16)
--     BRCLM.041        loss advice (BCL_LOSS_ADVICE), insurer update sources, document types (CLQ26)
--     NFR p.41         retention 10 years online, purge after 15 years (Q39, CLQ24)
--   Design: docs/architecture/CLAIMS_BROKING_DESIGN.md sections 4 (Flyway), 5.3 (set-up tables),
--   7 (security), 8 (status model, workflow BCL_CLAIM), 9 (parameters, LOVs, alerts, templates,
--   notification events) and 12.1 (lov_type.owner_permission). The permissions are the enum
--   security.domain.Permission; the grants are a proposal until OQ48 / CLQ04 / CLQ06 / CLQ15 are
--   answered. The claim tables come with V1021 (CL0) and V1022-V1024 (CL1-A / CL1-B); demo users
--   with V1920. The access-class rows of the document type CLAIM_REPORT are seeded by the
--   attachment access-class work item (EB V1031), which runs after this migration.
--   Runs after V750 (LOV), V751 (workflow), V754 (templates), V755 (sec_permission_action), V762
--   (notification events) and V790 (retention rules); references platform tables only.
-- =====================================================================================

-- ---------- Claims role profiles (design 7.2, stakeholder matrix p.28) ----------------------
insert into sec_role (code, name, created_at, created_by)
values ('CLM_OFFICER', 'Claims Officer / Claims Assistant', now(), 'SYSTEM'),
       ('CLM_TL', 'Claims Team Lead', now(), 'SYSTEM'),
       ('CLM_TH', 'Claims Team Head', now(), 'SYSTEM'),
       ('CLM_UH', 'Claims Unit Head', now(), 'SYSTEM'),
       ('CLM_RISK', 'Claims / Risk Analyst', now(), 'SYSTEM');

-- Every claims role: My Work, documents, report catalogue, client and account look-ups and the
-- Operations invoice pages (premium status of the cover).
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('WORK_VIEW'), ('ATTACHMENT_VIEW'), ('REPORT_VIEW'), ('CLIENT_VIEW'),
                   ('ACCOUNT_VIEW'), ('OPS_VIEW')) as p(permission)
where r.code in ('CLM_OFFICER', 'CLM_TL', 'CLM_TH', 'CLM_UH', 'CLM_RISK')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (design 7.2; FRS BRD-7 section 3.3).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Claims Officer / Assistant: records claims, authorization code, first and temporary-closure
    -- statuses (matrix), action plan, location references; documents and bulk uploads
    ('CLM_OFFICER', 'BCL_VIEW'), ('CLM_OFFICER', 'BCL_COVER_VIEW'), ('CLM_OFFICER', 'BCL_RECORD'),
    ('CLM_OFFICER', 'BCL_AUTHORIZE'), ('CLM_OFFICER', 'BCL_STATUS_UPDATE'),
    ('CLM_OFFICER', 'BCL_ACTION_PLAN'), ('CLM_OFFICER', 'BCL_LOCATION_REF_MAINTAIN'),
    ('CLM_OFFICER', 'BCL_REPORT_VIEW'), ('CLM_OFFICER', 'BCL_REPORT_EXPORT'),
    ('CLM_OFFICER', 'ATTACHMENT_MANAGE'), ('CLM_OFFICER', 'BULK_PROCESS'),
    -- Team Lead: + closure, claimant, settlement, adjuster, follow-up, reserve, reassignment
    ('CLM_TL', 'BCL_VIEW'), ('CLM_TL', 'BCL_COVER_VIEW'), ('CLM_TL', 'BCL_RECORD'),
    ('CLM_TL', 'BCL_AUTHORIZE'), ('CLM_TL', 'BCL_STATUS_UPDATE'), ('CLM_TL', 'BCL_CLOSE'),
    ('CLM_TL', 'BCL_CLAIMANT_OVERRIDE'), ('CLM_TL', 'BCL_SETTLEMENT_UPDATE'),
    ('CLM_TL', 'BCL_ADJUSTER_ASSIGN'), ('CLM_TL', 'BCL_FOLLOW_UP_OVERRIDE'),
    ('CLM_TL', 'BCL_RESERVE_AMEND'), ('CLM_TL', 'BCL_ACTION_PLAN'),
    ('CLM_TL', 'BCL_LOCATION_REF_MAINTAIN'), ('CLM_TL', 'BCL_REPORT_VIEW'),
    ('CLM_TL', 'BCL_REPORT_EXPORT'), ('CLM_TL', 'WORK_ASSIGN'), ('CLM_TL', 'ATTACHMENT_MANAGE'),
    ('CLM_TL', 'BULK_PROCESS'),
    -- Team Head: as the Team Lead + reopen (CLQ06)
    ('CLM_TH', 'BCL_VIEW'), ('CLM_TH', 'BCL_COVER_VIEW'), ('CLM_TH', 'BCL_RECORD'),
    ('CLM_TH', 'BCL_AUTHORIZE'), ('CLM_TH', 'BCL_STATUS_UPDATE'), ('CLM_TH', 'BCL_CLOSE'),
    ('CLM_TH', 'BCL_REOPEN'), ('CLM_TH', 'BCL_CLAIMANT_OVERRIDE'),
    ('CLM_TH', 'BCL_SETTLEMENT_UPDATE'), ('CLM_TH', 'BCL_ADJUSTER_ASSIGN'),
    ('CLM_TH', 'BCL_FOLLOW_UP_OVERRIDE'), ('CLM_TH', 'BCL_RESERVE_AMEND'),
    ('CLM_TH', 'BCL_ACTION_PLAN'), ('CLM_TH', 'BCL_LOCATION_REF_MAINTAIN'),
    ('CLM_TH', 'BCL_REPORT_VIEW'), ('CLM_TH', 'BCL_REPORT_EXPORT'), ('CLM_TH', 'WORK_ASSIGN'),
    ('CLM_TH', 'ATTACHMENT_MANAGE'), ('CLM_TH', 'BULK_PROCESS'),
    -- Unit Head: values and matrix (BCL_SETUP), reopen, reports and extract, reassignment
    ('CLM_UH', 'BCL_VIEW'), ('CLM_UH', 'BCL_COVER_VIEW'), ('CLM_UH', 'BCL_REOPEN'),
    ('CLM_UH', 'BCL_SETUP'), ('CLM_UH', 'BCL_REPORT_VIEW'), ('CLM_UH', 'BCL_REPORT_EXPORT'),
    ('CLM_UH', 'BCL_DATA_EXTRACT'), ('CLM_UH', 'WORK_ASSIGN'),
    -- Claims / Risk user: claims-prone locations and loss patterns (BRCLM.038)
    ('CLM_RISK', 'BCL_VIEW'), ('CLM_RISK', 'BCL_COVER_VIEW'), ('CLM_RISK', 'BCL_REPORT_VIEW'),
    ('CLM_RISK', 'BCL_REPORT_EXPORT'), ('CLM_RISK', 'BCL_DATA_EXTRACT'),
    -- Marketing: controlled report access only, never claim maintenance (BRCLM.040, CLQ15)
    ('MKT_AO', 'BCL_REPORT_VIEW'),
    ('MKT_TL', 'BCL_REPORT_VIEW'), ('MKT_TL', 'BCL_REPORT_EXPORT'),
    -- Platform administrator and auditor: read access and reports
    ('SYSADMIN', 'BCL_VIEW'), ('SYSADMIN', 'BCL_REPORT_VIEW'),
    ('AUDITOR', 'BCL_VIEW'), ('AUDITOR', 'BCL_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix, PMADD05) ---------------------------
insert into sec_permission_action (permission, area, action)
values
    ('BCL_VIEW', 'CLAIMS', 'VIEW'),
    ('BCL_COVER_VIEW', 'CLAIMS', 'VIEW'),
    ('BCL_REPORT_VIEW', 'CLAIMS', 'VIEW'),
    ('BCL_REPORT_EXPORT', 'CLAIMS', 'VIEW'),
    ('BCL_DATA_EXTRACT', 'CLAIMS', 'VIEW'),
    ('BCL_RECORD', 'CLAIMS', 'CREATE'),
    ('BCL_RECORD', 'CLAIMS', 'AMEND'),
    ('BCL_AUTHORIZE', 'CLAIMS', 'CREATE'),
    ('BCL_LOCATION_REF_MAINTAIN', 'CLAIMS', 'CREATE'),
    ('BCL_LOCATION_REF_MAINTAIN', 'CLAIMS', 'AMEND'),
    ('BCL_STATUS_UPDATE', 'CLAIMS', 'AMEND'),
    ('BCL_CLAIMANT_OVERRIDE', 'CLAIMS', 'AMEND'),
    ('BCL_SETTLEMENT_UPDATE', 'CLAIMS', 'AMEND'),
    ('BCL_ADJUSTER_ASSIGN', 'CLAIMS', 'AMEND'),
    ('BCL_FOLLOW_UP_OVERRIDE', 'CLAIMS', 'AMEND'),
    ('BCL_RESERVE_AMEND', 'CLAIMS', 'AMEND'),
    ('BCL_ACTION_PLAN', 'CLAIMS', 'AMEND'),
    ('BCL_SETUP', 'CLAIMS', 'AMEND'),
    ('BCL_CLOSE', 'CLAIMS', 'APPROVE'),
    ('BCL_REOPEN', 'CLAIMS', 'APPROVE');

-- ---------- Owner permission of a list of values (design 12.1) --------------------------------
-- When set, the values of the list may also be maintained (and authorized, by another user) with
-- this permission instead of the global LOV_MANAGE / MASTER_AUTHORIZE: the Claims Unit Head keeps
-- the Claims lists (BRCLM.010/014/017/036, p.28) without administering every list.
alter table lov_type add column owner_permission varchar(50);

-- ---------- Lists of values (design section 9.3) ----------------------------------------------
insert into lov_type (code, name, description, maintainable, owner_permission, created_at, created_by)
values ('BCL_CLAIM_STATUS', 'Claim status', 'BDOI claim status; phase and other attributes in bcl_lov_attribute (BRCLM.010, spec 6.1; CLQ04)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_SETTLEMENT_TYPE', 'Requested type of settlement', 'Settlement type of a claim; outcome and closure attributes in bcl_lov_attribute (BRCLM.014, spec 6.2; CLQ05)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_ADJUSTER', 'Adjuster / appraiser', 'Adjusters and appraisers of claims and insurer lines (BRCLM.017/018, spec 6.3; CLQ11)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_CATASTROPHE', 'Catastrophe code', 'Catastrophe event type of a loss (BRCLM.036, spec 6.4)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_LOSS_NATURE', 'Nature of loss', 'Nature of loss / accident of a claim (BRCLM.004, p.42; provisional, CLQ12)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_CLAIM_TYPE', 'Claim type', 'Type of claim (p.42; provisional, CLQ12)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_UNIT', 'Claims unit', 'Claims handling unit of a handler and a claim; used by the status access matrix (BRCLM.012, CLQ04)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_UPDATE_SOURCE', 'Insurer update source', 'Channel of an update received from an insurer (BRCLM.041, CLQ17)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_DIARY_TYPE', 'Claim diary entry type', 'Kind of a claim diary entry (BRCLM.022)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_DOCUMENT_TYPE', 'Claim document type', 'Document type of a claim; parent = DOCUMENT_TYPE code (p.24-25, CLQ26)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_REOPEN_REASON', 'Claim reopen reason', 'Why a permanently closed claim is reopened (BRCLM.035, CLQ06)', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_OVERRIDE_REASON', 'Claim override reason', 'Why a claimant, follow-up date or reported date is overridden (BRCLM.004/006/019)', true, 'BCL_SETUP', now(), 'SYSTEM');

-- Platform document type of a claims report (cross-BRD decision D3, BRCSF-009).
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
values ('DOCUMENT_TYPE', 'CLAIM_REPORT', 'Claims report', 78, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
        now(), now(), 'SYSTEM')
on conflict (type_code, code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, v.parent_code, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    -- Claim statuses (BRCLM.010, p.13-14; numbering of spec 6.1)
    ('BCL_CLAIM_STATUS', 'NEW_COMPLETE_DOCS', 'Newly Filed Claim - with complete documents', 10, null),
    ('BCL_CLAIM_STATUS', 'NEW_INCOMPLETE_DOCS', 'Newly Filed Claim - without or incomplete documents', 20, null),
    ('BCL_CLAIM_STATUS', 'ADJUSTER_REVIEW', 'For Adjuster''s Review and Evaluation', 30, null),
    ('BCL_CLAIM_STATUS', 'CLAIMANT_OFFER_ACCEPTANCE', 'For Claimant''s Acceptance of Offer', 40, null),
    ('BCL_CLAIM_STATUS', 'CLAIMANT_DOCS_SUBMISSION', 'For Claimant''s Submission of Documents', 50, null),
    ('BCL_CLAIM_STATUS', 'INSURER_CHECK_ISSUANCE', 'For Insurer''s Issuance of Check', 60, null),
    ('BCL_CLAIM_STATUS', 'INSURER_RELEASE_PAPERS', 'For Insurer''s Returning of Release Papers', 70, null),
    ('BCL_CLAIM_STATUS', 'INSURER_REVIEW', 'For Insurer''s Review and Evaluation', 80, null),
    ('BCL_CLAIM_STATUS', 'ADJUSTER_SETTLEMENT_OFFER', 'With Adjuster - For Issuance of Settlement Offer', 90, null),
    ('BCL_CLAIM_STATUS', 'ASSURED_MEETING', 'With Assured - For Schedule of Meeting', 100, null),
    ('BCL_CLAIM_STATUS', 'BDOI_PREMIUM_REMITTANCE', 'With BDOI - For Premium Remittance', 110, null),
    ('BCL_CLAIM_STATUS', 'BDOI_CHECK_TRANSMITTAL', 'With BDOI - For Transmittal of Settlement Check', 120, null),
    ('BCL_CLAIM_STATUS', 'BDOI_UNDER_REVIEW', 'With BDOI - Under Review / Discussion', 130, null),
    ('BCL_CLAIM_STATUS', 'CLAIMANT_SALVAGE_PULLOUT', 'With Claimant - For Pull-out of Salvaged Items', 140, null),
    ('BCL_CLAIM_STATUS', 'CLAIMANT_CNR_SUBMISSION', 'With Claimant - For Submission of CNR', 150, null),
    ('BCL_CLAIM_STATUS', 'INSURER_LOA_ISSUANCE', 'With Insurer - For Issuance of LOA', 160, null),
    ('BCL_CLAIM_STATUS', 'TEMP_CLOSED_NO_DOCS', 'Temporary Closed Claim - Non-submission of Documents', 170, null),
    ('BCL_CLAIM_STATUS', 'TEMP_CLOSED_WITH_OFFER', 'Temporary Closed Claim - with Offer', 180, null),
    -- Requested types of settlement (BRCLM.014, p.14; spec 6.2)
    ('BCL_SETTLEMENT_TYPE', 'CLOSED_CANCELLED', 'Closed - Cancelled', 10, null),
    ('BCL_SETTLEMENT_TYPE', 'CLOSED_DENIED', 'Closed - Denied', 20, null),
    ('BCL_SETTLEMENT_TYPE', 'CLOSED_WITHIN_DEDUCTIBLE', 'Closed - within Deductible', 30, null),
    ('BCL_SETTLEMENT_TYPE', 'CLOSED_WITHOUT_PAYMENT', 'Closed - without Payment', 40, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED', 'Settled', 50, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED_DIRECTLY_FILED', 'Settled - Directly Filed', 60, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED_LOA_ISSUED', 'Settled - LOA Issued', 70, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED_LOA_REPAIR_SCHEDULE', 'Settled - LOA Issued - For Schedule of Repair', 80, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED_LOA_UNDER_REPAIR', 'Settled - LOA Issued - Vehicle Under Repair', 90, null),
    ('BCL_SETTLEMENT_TYPE', 'SETTLED_RELEASE_PAPERS', 'Settled - Signed Release Papers Returned', 100, null),
    -- Adjusters / appraisers (BRCLM.017, p.14-15; contacts parked, CLQ11)
    ('BCL_ADJUSTER', 'GEMINI', 'Gemini Adjustment Company', 10, null),
    ('BCL_ADJUSTER', 'CHARTERED', 'Chartered Adjusters, Inc.', 20, null),
    ('BCL_ADJUSTER', 'BA_INTERNATIONAL', 'BA International Adjusters & Surveyors Company, Inc.', 30, null),
    ('BCL_ADJUSTER', 'TOTAL_CLAIMS', 'Total Claims Specialist, Inc.', 40, null),
    ('BCL_ADJUSTER', 'UNIFIED', 'Unified Adjusters and Surveyors (FAR East), Inc.', 50, null),
    ('BCL_ADJUSTER', 'TOP_BRASS', 'Top Brass Insurance Adjusters & Surveyors Co., Inc.', 60, null),
    ('BCL_ADJUSTER', 'ASCOR', 'Adjustment Standard Corporation (ASCOR)', 70, null),
    ('BCL_ADJUSTER', 'CARES', 'CARES Adjusters & Surveyors, Inc.', 80, null),
    ('BCL_ADJUSTER', 'PLARIDEL', 'Plaridel Adjusters and Appraisers, Inc.', 90, null),
    ('BCL_ADJUSTER', 'CRAWFORD', 'Crawford & Company Philippines, Inc.', 100, null),
    ('BCL_ADJUSTER', 'MASCO', 'Manila Adjusters & Surveyors Company (MASCO)', 110, null),
    ('BCL_ADJUSTER', 'TAN_GATUE', 'Tan-Gatue Adjustment Company, Inc.', 120, null),
    ('BCL_ADJUSTER', 'TECHNICAL_INSPECTION', 'Technical Inspection Group Adjustment & Surveyors Corp.', 130, null),
    ('BCL_ADJUSTER', 'ESTEBAN', 'Esteban Adjusters and Valuers, Inc.', 140, null),
    ('BCL_ADJUSTER', 'INTERCLAIM', 'Interclaim Adjustment Co., Inc.', 150, null),
    ('BCL_ADJUSTER', 'PACIFIC_INTERNATIONAL', 'Pacific International Loss Adjusters Co., Inc.', 160, null),
    ('BCL_ADJUSTER', 'SENON', 'Senon Insurance Adjusters & Appraisers', 170, null),
    ('BCL_ADJUSTER', 'AUDEMUS', 'Audemus Adjustment Corporation', 180, null),
    ('BCL_ADJUSTER', 'PALM', 'PALM Property Adjusters', 190, null),
    ('BCL_ADJUSTER', 'MCLARENS', 'McLarens Philippines', 200, null),
    ('BCL_ADJUSTER', 'UNIVERSAL', 'Universal Adjuster Appraisers Co., Inc.', 210, null),
    ('BCL_ADJUSTER', 'EAGLE_PROSPERITY', 'Eagle Prosperity Adjustment and Surveyor Corp.', 220, null),
    ('BCL_ADJUSTER', 'DFM', 'DFM Adjustment Co., Inc.', 230, null),
    ('BCL_ADJUSTER', 'OPTIMUM', 'Optimum Claims Solutions Insurance Adjustment, Inc.', 240, null),
    ('BCL_ADJUSTER', 'TREBORASIA', 'TreborAsia Insurance Adjustment Services', 250, null),
    -- Catastrophe codes (BRCLM.036, p.16)
    ('BCL_CATASTROPHE', 'TYPHOON', 'Typhoon', 10, null),
    ('BCL_CATASTROPHE', 'EARTHQUAKE', 'Earthquake', 20, null),
    ('BCL_CATASTROPHE', 'FLOOD', 'Flood', 30, null),
    ('BCL_CATASTROPHE', 'VOLCANIC_ERUPTION', 'Volcanic Eruption', 40, null),
    ('BCL_CATASTROPHE', 'LANDSLIDE', 'Landslide', 50, null),
    ('BCL_CATASTROPHE', 'FIRE', 'Fire', 60, null),
    ('BCL_CATASTROPHE', 'OTHERS', 'Others (i.e. Pandemic, El Nino, Terrorism)', 90, null),
    -- Nature of loss and claim type: provisional seeds until BDOI gives the lists (CLQ12)
    ('BCL_LOSS_NATURE', 'MOTOR_OWN_DAMAGE', 'Motor own damage (to confirm)', 10, null),
    ('BCL_LOSS_NATURE', 'MOTOR_THIRD_PARTY', 'Motor third party (to confirm)', 20, null),
    ('BCL_LOSS_NATURE', 'MOTOR_THEFT', 'Motor theft (to confirm)', 30, null),
    ('BCL_LOSS_NATURE', 'FIRE', 'Fire (to confirm)', 40, null),
    ('BCL_LOSS_NATURE', 'PROPERTY', 'Property (to confirm)', 50, null),
    ('BCL_LOSS_NATURE', 'ENGINEERING', 'Engineering (to confirm)', 60, null),
    ('BCL_LOSS_NATURE', 'MARINE', 'Marine (to confirm)', 70, null),
    ('BCL_LOSS_NATURE', 'LIABILITY', 'Liability (to confirm)', 80, null),
    ('BCL_LOSS_NATURE', 'PERSONAL_ACCIDENT', 'Personal accident (to confirm)', 90, null),
    ('BCL_LOSS_NATURE', 'OTHERS', 'Others', 990, null),
    ('BCL_CLAIM_TYPE', 'MOTOR_OWN_DAMAGE', 'Motor own damage (to confirm)', 10, null),
    ('BCL_CLAIM_TYPE', 'MOTOR_THIRD_PARTY', 'Motor third party (to confirm)', 20, null),
    ('BCL_CLAIM_TYPE', 'MOTOR_THEFT', 'Motor theft (to confirm)', 30, null),
    ('BCL_CLAIM_TYPE', 'FIRE', 'Fire (to confirm)', 40, null),
    ('BCL_CLAIM_TYPE', 'PROPERTY', 'Property (to confirm)', 50, null),
    ('BCL_CLAIM_TYPE', 'ENGINEERING', 'Engineering (to confirm)', 60, null),
    ('BCL_CLAIM_TYPE', 'MARINE', 'Marine (to confirm)', 70, null),
    ('BCL_CLAIM_TYPE', 'LIABILITY', 'Liability (to confirm)', 80, null),
    ('BCL_CLAIM_TYPE', 'PERSONAL_ACCIDENT', 'Personal accident (to confirm)', 90, null),
    ('BCL_CLAIM_TYPE', 'OTHERS', 'Others', 990, null),
    -- Claims units: head office Motor / Non-Motor and the five branches (NFR locations; CLQ04)
    ('BCL_UNIT', 'MOTOR_HO', 'Motor - Head Office', 10, null),
    ('BCL_UNIT', 'NON_MOTOR_HO', 'Non-Motor - Head Office', 20, null),
    ('BCL_UNIT', 'BRANCH_ANGELES', 'Branch - Angeles', 30, null),
    ('BCL_UNIT', 'BRANCH_CEBU', 'Branch - Cebu', 40, null),
    ('BCL_UNIT', 'BRANCH_CDO', 'Branch - Cagayan de Oro', 50, null),
    ('BCL_UNIT', 'BRANCH_DAVAO', 'Branch - Davao', 60, null),
    ('BCL_UNIT', 'BRANCH_GENSAN', 'Branch - General Santos', 70, null),
    -- Insurer update sources (BRCLM.041; channels parked, CLQ17)
    ('BCL_UPDATE_SOURCE', 'EMAIL', 'E-mail', 10, null),
    ('BCL_UPDATE_SOURCE', 'LETTER', 'Letter', 20, null),
    ('BCL_UPDATE_SOURCE', 'PORTAL', 'Insurer portal', 30, null),
    ('BCL_UPDATE_SOURCE', 'CALL', 'Call', 40, null),
    ('BCL_UPDATE_SOURCE', 'FILE', 'File from the insurer', 50, null),
    -- Diary entry types (BRCLM.022)
    ('BCL_DIARY_TYPE', 'CALL', 'Call', 10, null),
    ('BCL_DIARY_TYPE', 'EMAIL', 'E-mail', 20, null),
    ('BCL_DIARY_TYPE', 'MEETING', 'Meeting', 30, null),
    ('BCL_DIARY_TYPE', 'NOTE', 'Note', 40, null),
    ('BCL_DIARY_TYPE', 'FOLLOW_UP', 'Follow-up', 50, null),
    -- Claim document types, each mapped to its attachment document type (p.24-25; CLQ26)
    ('BCL_DOCUMENT_TYPE', 'PLA', 'Preliminary Loss Advice (PLA)', 10, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'CRF', 'Claims Reporting Form (CRF)', 20, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'ESTIMATE', 'Repair estimate', 30, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'OFFER', 'Settlement offer', 40, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'SIGNED_OFFER', 'Signed offer', 50, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'LOA', 'Letter of Authority (LOA)', 60, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'RELEASE_PAPERS', 'Release papers', 70, 'OTHERS'),
    ('BCL_DOCUMENT_TYPE', 'CLAIM_REPORT', 'Claims report', 80, 'CLAIM_REPORT'),
    ('BCL_DOCUMENT_TYPE', 'OTHERS', 'Others', 90, 'OTHERS'),
    -- Reopen reasons (BRCLM.035; BDOI supplies its own, CLQ06)
    ('BCL_REOPEN_REASON', 'INSURER_RECONSIDERATION', 'Insurer reconsidered the claim', 10, null),
    ('BCL_REOPEN_REASON', 'ADDITIONAL_LOSS', 'Additional loss or documents received', 20, null),
    ('BCL_REOPEN_REASON', 'CLOSED_IN_ERROR', 'Closed in error', 30, null),
    ('BCL_REOPEN_REASON', 'OTHERS', 'Others (see remarks)', 90, null),
    -- Override reasons (BRCLM.004/006/019)
    ('BCL_OVERRIDE_REASON', 'DATA_CORRECTION', 'Data correction', 10, null),
    ('BCL_OVERRIDE_REASON', 'INSURER_ADVICE', 'Advice from the insurer', 20, null),
    ('BCL_OVERRIDE_REASON', 'CLIENT_REQUEST', 'Request of the client', 30, null),
    ('BCL_OVERRIDE_REASON', 'OTHERS', 'Others (see remarks)', 90, null)
) as v(type_code, code, label, sort_order, parent_code);

-- Attributes of the Claims LOV values (BRCLM.010/014; spec 6.1-6.2). lov_value has no attribute
-- columns, so Claims keeps them by (type, code, attribute), as Collections does. Owned by
-- brokerclaims; maintained on Claims Setup (BCL_SETUP, wave CL1-B).
--   BCL_CLAIM_STATUS: phase (NEW / IN_PROGRESS / TEMP_CLOSED / CLOSED = enum ClaimPhase),
--     waiting_on (INSURER / CLAIMANT / ADJUSTER / ASSURED / BDOI), follow_up_days (integer; absent
--     = parameter BCL_FOLLOW_UP_DAYS), awaiting_premium_remittance (true / false; absent = false)
--   BCL_SETTLEMENT_TYPE: outcome (SETTLED / CLOSED_WITHOUT_PAYMENT), closes_claim (true / false),
--     requires_settlement_amount (true / false)
create table bcl_lov_attribute (
    id          bigint generated by default as identity primary key,
    version     bigint        not null default 0,
    type_code   varchar(40)   not null references lov_type (code),
    code        varchar(40)   not null,
    attribute   varchar(40)   not null,
    value       varchar(200)  not null,
    created_at  timestamptz   not null,
    created_by  varchar(50)   not null,
    updated_at  timestamptz,
    updated_by  varchar(50),
    constraint uq_bcl_lov_attribute unique (type_code, code, attribute),
    constraint fk_bcl_lov_attribute_value foreign key (type_code, code) references lov_value (type_code, code),
    constraint ck_bcl_lov_attribute_name check (attribute in ('phase', 'waiting_on', 'follow_up_days',
        'awaiting_premium_remittance', 'outcome', 'closes_claim', 'requires_settlement_amount'))
);

insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select 'BCL_CLAIM_STATUS', s.code, a.attribute, a.value, now(), 'SYSTEM'
from (values
    ('NEW_COMPLETE_DOCS', 'NEW', 'INSURER'),
    ('NEW_INCOMPLETE_DOCS', 'NEW', 'CLAIMANT'),
    ('ADJUSTER_REVIEW', 'IN_PROGRESS', 'ADJUSTER'),
    ('CLAIMANT_OFFER_ACCEPTANCE', 'IN_PROGRESS', 'CLAIMANT'),
    ('CLAIMANT_DOCS_SUBMISSION', 'IN_PROGRESS', 'CLAIMANT'),
    ('INSURER_CHECK_ISSUANCE', 'IN_PROGRESS', 'INSURER'),
    ('INSURER_RELEASE_PAPERS', 'IN_PROGRESS', 'INSURER'),
    ('INSURER_REVIEW', 'IN_PROGRESS', 'INSURER'),
    ('ADJUSTER_SETTLEMENT_OFFER', 'IN_PROGRESS', 'ADJUSTER'),
    ('ASSURED_MEETING', 'IN_PROGRESS', 'ASSURED'),
    ('BDOI_PREMIUM_REMITTANCE', 'IN_PROGRESS', 'BDOI'),
    ('BDOI_CHECK_TRANSMITTAL', 'IN_PROGRESS', 'BDOI'),
    ('BDOI_UNDER_REVIEW', 'IN_PROGRESS', 'BDOI'),
    ('CLAIMANT_SALVAGE_PULLOUT', 'IN_PROGRESS', 'CLAIMANT'),
    ('CLAIMANT_CNR_SUBMISSION', 'IN_PROGRESS', 'CLAIMANT'),
    ('INSURER_LOA_ISSUANCE', 'IN_PROGRESS', 'INSURER'),
    ('TEMP_CLOSED_NO_DOCS', 'TEMP_CLOSED', 'CLAIMANT'),
    ('TEMP_CLOSED_WITH_OFFER', 'TEMP_CLOSED', 'CLAIMANT')
) as s(code, phase, waiting_on)
cross join lateral (values ('phase', s.phase), ('waiting_on', s.waiting_on)) as a(attribute, value);

-- Status 11 feeds the claims special remittance (ClaimsFeed CLAIMS_SPECIAL_REMIT; OQ46, OQ25).
insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
values ('BCL_CLAIM_STATUS', 'BDOI_PREMIUM_REMITTANCE', 'awaiting_premium_remittance', 'true', now(), 'SYSTEM');

-- Settlement types. The two "LOA Issued - For Schedule of Repair / Vehicle Under Repair" types
-- keep the claim open until BDOI confirms whether they close it (CLQ05).
insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select 'BCL_SETTLEMENT_TYPE', s.code, a.attribute, a.value, now(), 'SYSTEM'
from (values
    ('CLOSED_CANCELLED', 'CLOSED_WITHOUT_PAYMENT', 'true', 'false'),
    ('CLOSED_DENIED', 'CLOSED_WITHOUT_PAYMENT', 'true', 'false'),
    ('CLOSED_WITHIN_DEDUCTIBLE', 'CLOSED_WITHOUT_PAYMENT', 'true', 'false'),
    ('CLOSED_WITHOUT_PAYMENT', 'CLOSED_WITHOUT_PAYMENT', 'true', 'false'),
    ('SETTLED', 'SETTLED', 'true', 'true'),
    ('SETTLED_DIRECTLY_FILED', 'SETTLED', 'true', 'true'),
    ('SETTLED_LOA_ISSUED', 'SETTLED', 'true', 'true'),
    ('SETTLED_LOA_REPAIR_SCHEDULE', 'SETTLED', 'false', 'true'),
    ('SETTLED_LOA_UNDER_REPAIR', 'SETTLED', 'false', 'true'),
    ('SETTLED_RELEASE_PAPERS', 'SETTLED', 'true', 'true')
) as s(code, outcome, closes, requires_amount)
cross join lateral (values ('outcome', s.outcome), ('closes_claim', s.closes),
                           ('requires_settlement_amount', s.requires_amount)) as a(attribute, value);

-- ---------- Status access matrix (BRCLM.012/013; design 5.3) ----------------------------------
-- A status may be set by a user holding one of the roles of its rows, in the unit of the row
-- (null = any unit; the user's unit comes from bcl_handler). Maker-checker (AuthorizableEntity):
-- changes wait for another BCL_SETUP user. The default until BDOI gives the matrix (CLQ04):
-- officers set the newly filed and temporary-closure statuses, Team Leads and Team Heads every one.
create table bcl_status_access (
    id             bigint generated by default as identity primary key,
    version        bigint       not null default 0,
    status_code    varchar(40)  not null,
    status_type    varchar(40)  not null default 'BCL_CLAIM_STATUS',
    role_code      varchar(40)  not null references sec_role (code),
    unit_code      varchar(40),
    record_status  varchar(30)  not null,
    authorized_by  varchar(50),
    authorized_at  timestamptz,
    created_at     timestamptz  not null,
    created_by     varchar(50)  not null,
    updated_at     timestamptz,
    updated_by     varchar(50),
    constraint fk_bcl_status_access_status foreign key (status_type, status_code)
        references lov_value (type_code, code),
    constraint ck_bcl_status_access_type check (status_type = 'BCL_CLAIM_STATUS'),
    constraint ck_bcl_status_access_record check (record_status in ('PENDING_AUTHORIZATION', 'ACTIVE', 'INACTIVE'))
);
create unique index uq_bcl_status_access on bcl_status_access (status_code, role_code, coalesce(unit_code, ''));
create index ix_bcl_status_access_role on bcl_status_access (role_code, record_status);

insert into bcl_status_access (status_code, role_code, unit_code, record_status, authorized_by,
                               authorized_at, created_at, created_by)
select s.code, r.role_code, null, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from lov_value s
cross join (values ('CLM_OFFICER'), ('CLM_TL'), ('CLM_TH')) as r(role_code)
where s.type_code = 'BCL_CLAIM_STATUS'
  and (r.role_code <> 'CLM_OFFICER'
       or s.code in ('NEW_COMPLETE_DOCS', 'NEW_INCOMPLETE_DOCS', 'TEMP_CLOSED_NO_DOCS',
                     'TEMP_CLOSED_WITH_OFFER'));

-- ---------- Claims handler register (BRCLM.012, NFR p.37; design 5.3) -------------------------
-- The unit of a handler drives the status access matrix and the default assignment; the team is
-- shown on the reports. Handlers are sec_user usernames (plain values).
create table bcl_handler (
    id          bigint generated by default as identity primary key,
    version     bigint       not null default 0,
    username    varchar(50)  not null,
    unit_code   varchar(40)  not null,
    unit_type   varchar(40)  not null default 'BCL_UNIT',
    team        varchar(60),
    active      boolean      not null default true,
    created_at  timestamptz  not null,
    created_by  varchar(50)  not null,
    updated_at  timestamptz,
    updated_by  varchar(50),
    constraint uq_bcl_handler unique (username),
    constraint fk_bcl_handler_unit foreign key (unit_type, unit_code) references lov_value (type_code, code),
    constraint ck_bcl_handler_unit_type check (unit_type = 'BCL_UNIT')
);
create index ix_bcl_handler_unit on bcl_handler (unit_code, active);

-- ---------- Business parameters (design 9.3) --------------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('BCL_DEFAULT_CURRENCY', 'PHP', 'STRING', 'CLAIMS_HANDLING',
     'Currency of a claim whose cover has none (BRCLM.009)', null, null, now(), 'SYSTEM'),
    ('BCL_FOLLOW_UP_DAYS', '7', 'INTEGER', 'CLAIMS_HANDLING',
     'Days from a status change to the next follow-up date when the status has no follow_up_days attribute (BRCLM.019; CLQ07)', 1, 365, now(), 'SYSTEM'),
    ('BCL_AGEING_BUCKETS', '30,60,90,180', 'INTEGER_LIST', 'CLAIMS_HANDLING',
     'Upper bounds in days of the claims ageing buckets: 0-30, 31-60, 61-90, 91-180, 181+ (BRCLM.025/026)', 1, 3650, now(), 'SYSTEM'),
    ('BCL_PAST_DUE_DAYS', '90', 'INTEGER', 'CLAIMS_HANDLING',
     'Age in days above which an outstanding claim is past due: report and alert BCL_CLAIM_PAST_DUE (BRCLM.031, p.43)', 1, 3650, now(), 'SYSTEM'),
    ('BCL_PRONE_MIN_CLAIMS', '3', 'INTEGER', 'CLAIMS_HANDLING',
     'Claims at one location that make it claims-prone (BRCLM.038; CLQ16)', 1, 100, now(), 'SYSTEM'),
    ('BCL_PRONE_YEARS', '3', 'INTEGER', 'CLAIMS_HANDLING',
     'Look-back years of the claims-prone location analysis (BRCLM.038; CLQ16)', 1, 20, now(), 'SYSTEM'),
    ('BCL_AUTH_DP_POLICY', 'CONFIRM', 'STRING', 'CLAIMS_HANDLING',
     'Claims authorization code on a direct-payment cover: ALLOW, CONFIRM (insurer payment evidence attached) or BLOCK (BRCLM.001; CLQ01)', null, null, now(), 'SYSTEM');

-- ---------- Exception codes (design 9.2) ------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('BCL_UNPAID_PREMIUM_CLAIM', 'Claim on unpaid premium',
     'A claim is recorded on, or stays on, a cover with unpaid or partly paid premium (BRCLM.001).',
     'CLAIMS_HANDLING', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('BCL_FOLLOW_UP_OVERDUE', 'Claim follow-up overdue',
     'The next follow-up date or a diary due date of a claim has passed (BRCLM.019/022/034).',
     'CLAIMS_HANDLING', 'LOW', null, 0, now(), 'SYSTEM'),
    ('BCL_CLAIM_PAST_DUE', 'Outstanding claim past due',
     'An outstanding claim is older than BCL_PAST_DUE_DAYS (BRCLM.031, p.43).',
     'CLAIMS_HANDLING', 'MEDIUM', null, 90, now(), 'SYSTEM'),
    ('BCL_INSURER_CLAIM_NO_REUSED', 'Insurer claim number reused',
     'An insurer claim number already exists on another claim for the same insurer (BRCLM.043).',
     'CLAIMS_HANDLING', 'LOW', null, null, now(), 'SYSTEM');

-- ---------- Notification events (design 9.4; wording and e-mail recipients CLQ22) -------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('BCL_FOLLOW_UP_DUE', 'Claim follow-up due', 'CLAIMS_HANDLING',
     'A claim of yours has its next follow-up or a diary entry due today (BRCLM.019/022/034)', true, false, 400),
    ('BCL_CLAIM_ASSIGNED', 'Claim assigned', 'CLAIMS_HANDLING',
     'A claim was assigned or re-assigned to you (NFR p.37)', true, true, 410),
    ('BCL_STATUS_CHANGED', 'Claim status changed', 'CLAIMS_HANDLING',
     'The status of a claim of one of your accounts changed (BRCLM.011; NFR 15.14)', true, false, 420),
    ('BCL_PREMIUM_REMITTED', 'Claim premium remitted', 'CLAIMS_HANDLING',
     'The premium of a claim awaiting premium remittance was fully remitted to the insurer (BRCLM.001, OQ46)', true, false, 430),
    ('BCL_NEWER_COVER_VERSION', 'Newer cover version', 'CLAIMS_HANDLING',
     'An endorsement was booked on the cover of an open claim of yours (BRCLM.039)', true, false, 440);

-- ---------- Workflow BCL_CLAIM (design 8.2; FRS BRD-7 section 5.1) ----------------------------
-- The stage mirrors the claim phase. Every transition is a business action called only by the
-- Claims services (systemTransition after their own permission and matrix checks); the permission
-- column documents the right. CLOSED is not terminal because a closed claim can be reopened.
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('BCL_CLAIM', 'NEW', 'Newly filed', 'BCL_RECORD', 24, true, false, 10),
       ('BCL_CLAIM', 'IN_PROGRESS', 'In progress', 'BCL_RECORD', null, false, false, 20),
       ('BCL_CLAIM', 'TEMP_CLOSED', 'Temporarily closed', null, null, false, false, 30),
       ('BCL_CLAIM', 'CLOSED', 'Closed', null, null, false, false, 40);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('BCL_CLAIM', 'NEW', 'progress', 'IN_PROGRESS', 'Set an in-progress status', 'BCL_STATUS_UPDATE', false, null, 10),
       ('BCL_CLAIM', 'NEW', 'temp_close', 'TEMP_CLOSED', 'Close temporarily', 'BCL_STATUS_UPDATE', false, null, 20),
       ('BCL_CLAIM', 'NEW', 'close', 'CLOSED', 'Close with a settlement type', 'BCL_CLOSE', false, null, 30),
       ('BCL_CLAIM', 'IN_PROGRESS', 'temp_close', 'TEMP_CLOSED', 'Close temporarily', 'BCL_STATUS_UPDATE', false, null, 10),
       ('BCL_CLAIM', 'IN_PROGRESS', 'close', 'CLOSED', 'Close with a settlement type', 'BCL_CLOSE', false, null, 20),
       ('BCL_CLAIM', 'TEMP_CLOSED', 'resume', 'IN_PROGRESS', 'Resume the claim', 'BCL_STATUS_UPDATE', false, null, 10),
       ('BCL_CLAIM', 'TEMP_CLOSED', 'close', 'CLOSED', 'Close with a settlement type', 'BCL_CLOSE', false, null, 20),
       ('BCL_CLAIM', 'CLOSED', 'reopen', 'IN_PROGRESS', 'Reopen the claim', 'BCL_REOPEN', false, 'BCL_REOPEN_REASON', 10);

-- ---------- Loss advice template (design 9.4, spec 6.5; wording CLQ22) -------------------------
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('BCL_LOSS_ADVICE', 1, 'Loss advice {{claimNo}} - {{assuredName}}',
 'Dear {{insurerName}} Claims Team,

We are notifying you of a loss under the policy below and request that the claim be registered.

Assured: {{assuredName}}
Policy / reference no.: {{policyNo}} (ARN {{arn}}, policy year {{policyYear}})
Date of accident / loss: {{lossDate}}
Location of accident / loss: {{lossPlace}}
Nature of loss / description: {{lossNature}} - {{lossDescription}}
Initial loss reserve: {{currency}} {{initialReserve}}
Assigned adjuster: {{adjusterName}}
Your claim number(s): {{insurerClaimNos}}

Our claim reference is {{claimNo}}; please quote it in your correspondence. Supporting documents are attached where available.

Thank you,
{{handlerName}}
BDO Insurance and Reinsurance Brokers, Inc. - Claims', date '2020-01-01', now(), 'SYSTEM');

-- ---------- Retention (NFR p.41: 10 years online, purge after 15 years; Q39, CLQ24) -----------
-- The review counts closed claims (phase CLOSED) through BrokerClaimRetentionProvider (CL1-A);
-- until then the review run records "no provider". Archive and purge stay parked.
insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
values ('BROKER_CLAIM', 'CLOSED', 10, 5, 'REVIEW', true,
        'Closed broking claims with their insurer lines, updates, diary and documents: 10 years online, purge after 15 years (BRD-7 p.41)', now(), 'SYSTEM');
