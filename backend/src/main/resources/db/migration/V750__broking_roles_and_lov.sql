-- =====================================================================================
-- iNXT BrokerVerse - V750 Broking foundations: BDOI roles and permissions, lists of values.
--   Requirements: BRD-1 New Business (docs/requirements/BDOI_NB_BRD_SPEC.md)
--     BRNB.040/084/088  role profiles and functions per persona
--     BRNB.083          lists of values with effectivity, maintained by business administrators
--   Architecture: docs/architecture/BROKING_ARCHITECTURE.md sections 3.2 and 3.7.
-- =====================================================================================

-- ---------- BDOI role profiles (BRD personas) -----------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('MKT_AO', 'Marketing Account Officer', now(), 'SYSTEM'),
       ('MKT_TL', 'Marketing Team Leader / Head (approver)', now(), 'SYSTEM'),
       ('TSU', 'Technical Support Unit', now(), 'SYSTEM'),
       ('PROCESSOR', 'Processing', now(), 'SYSTEM'),
       ('PROCESSING_TL', 'Processing Team Leader', now(), 'SYSTEM'),
       ('NB_APPROVER', 'Approver (requests, workload)', now(), 'SYSTEM'),
       ('EPOLICY_SENDER', 'E-policy Sender', now(), 'SYSTEM'),
       ('ADJUSTMENT', 'Adjustment', now(), 'SYSTEM'),
       ('BUSINESS_ADMIN', 'Business Administrator', now(), 'SYSTEM');

-- Grants: (role, permission) pairs.
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Marketing Account Officer
    ('MKT_AO', 'CLIENT_VIEW'), ('MKT_AO', 'CLIENT_MAINTAIN'), ('MKT_AO', 'QUOTE_VIEW'),
    ('MKT_AO', 'QUOTE_MAINTAIN'), ('MKT_AO', 'PROPOSAL_REQUEST'), ('MKT_AO', 'ACCOUNT_VIEW'),
    ('MKT_AO', 'ACCOUNT_MAINTAIN'), ('MKT_AO', 'BULK_PROCESS'), ('MKT_AO', 'WORK_VIEW'),
    ('MKT_AO', 'ATTACHMENT_VIEW'), ('MKT_AO', 'ATTACHMENT_MANAGE'), ('MKT_AO', 'REPORT_VIEW'),
    -- Marketing TL / TH / UH
    ('MKT_TL', 'CLIENT_VIEW'), ('MKT_TL', 'CLIENT_MAINTAIN'), ('MKT_TL', 'CLIENT_APPROVE'),
    ('MKT_TL', 'QUOTE_VIEW'), ('MKT_TL', 'QUOTE_MAINTAIN'), ('MKT_TL', 'QUOTE_APPROVE'),
    ('MKT_TL', 'PROPOSAL_REQUEST'), ('MKT_TL', 'PROPOSAL_APPROVE'), ('MKT_TL', 'ACCOUNT_VIEW'),
    ('MKT_TL', 'ACCOUNT_MAINTAIN'), ('MKT_TL', 'BULK_PROCESS'), ('MKT_TL', 'WORK_VIEW'),
    ('MKT_TL', 'WORK_ASSIGN'), ('MKT_TL', 'ATTACHMENT_VIEW'), ('MKT_TL', 'ATTACHMENT_MANAGE'),
    ('MKT_TL', 'REPORT_VIEW'), ('MKT_TL', 'DASHBOARD_VIEW'),
    -- Technical Support Unit
    ('TSU', 'CLIENT_VIEW'), ('TSU', 'QUOTE_VIEW'), ('TSU', 'TSU_PROCESS'), ('TSU', 'TSU_APPROVE'),
    ('TSU', 'ACCOUNT_VIEW'), ('TSU', 'WORK_VIEW'), ('TSU', 'ATTACHMENT_VIEW'),
    ('TSU', 'ATTACHMENT_MANAGE'), ('TSU', 'REPORT_VIEW'),
    -- Processing
    ('PROCESSOR', 'CLIENT_VIEW'), ('PROCESSOR', 'QUOTE_VIEW'), ('PROCESSOR', 'ACCOUNT_VIEW'),
    ('PROCESSOR', 'ACCOUNT_MAINTAIN'), ('PROCESSOR', 'ACCOUNT_PROCESS'),
    ('PROCESSOR', 'PLACEMENT_MANAGE'), ('PROCESSOR', 'BILLING_MANAGE'),
    ('PROCESSOR', 'EPOLICY_MANAGE'), ('PROCESSOR', 'BOOKING_PROCESS'),
    ('PROCESSOR', 'BULK_PROCESS'), ('PROCESSOR', 'WORK_VIEW'), ('PROCESSOR', 'ATTACHMENT_VIEW'),
    ('PROCESSOR', 'ATTACHMENT_MANAGE'), ('PROCESSOR', 'REPORT_VIEW'),
    -- Processing team leader
    ('PROCESSING_TL', 'CLIENT_VIEW'), ('PROCESSING_TL', 'QUOTE_VIEW'),
    ('PROCESSING_TL', 'ACCOUNT_VIEW'), ('PROCESSING_TL', 'ACCOUNT_MAINTAIN'),
    ('PROCESSING_TL', 'ACCOUNT_PROCESS'), ('PROCESSING_TL', 'PLACEMENT_MANAGE'),
    ('PROCESSING_TL', 'BILLING_MANAGE'), ('PROCESSING_TL', 'EPOLICY_MANAGE'),
    ('PROCESSING_TL', 'BOOKING_PROCESS'), ('PROCESSING_TL', 'BULK_PROCESS'),
    ('PROCESSING_TL', 'WORK_VIEW'), ('PROCESSING_TL', 'WORK_ASSIGN'),
    ('PROCESSING_TL', 'ATTACHMENT_VIEW'), ('PROCESSING_TL', 'ATTACHMENT_MANAGE'),
    ('PROCESSING_TL', 'REPORT_VIEW'), ('PROCESSING_TL', 'DASHBOARD_VIEW'),
    -- Approver (BRD 3.1): pending requests, workload assignment
    ('NB_APPROVER', 'CLIENT_VIEW'), ('NB_APPROVER', 'QUOTE_VIEW'), ('NB_APPROVER', 'QUOTE_APPROVE'),
    ('NB_APPROVER', 'ACCOUNT_VIEW'), ('NB_APPROVER', 'WORK_VIEW'), ('NB_APPROVER', 'WORK_ASSIGN'),
    ('NB_APPROVER', 'ACCESS_APPROVE'), ('NB_APPROVER', 'MASTER_VIEW'),
    ('NB_APPROVER', 'MASTER_AUTHORIZE'), ('NB_APPROVER', 'ATTACHMENT_VIEW'),
    ('NB_APPROVER', 'REPORT_VIEW'), ('NB_APPROVER', 'DASHBOARD_VIEW'),
    -- E-policy Sender
    ('EPOLICY_SENDER', 'CLIENT_VIEW'), ('EPOLICY_SENDER', 'ACCOUNT_VIEW'),
    ('EPOLICY_SENDER', 'EPOLICY_SEND'), ('EPOLICY_SENDER', 'WORK_VIEW'),
    ('EPOLICY_SENDER', 'ATTACHMENT_VIEW'), ('EPOLICY_SENDER', 'REPORT_VIEW'),
    -- Adjustment (BRD 3.2)
    ('ADJUSTMENT', 'CLIENT_VIEW'), ('ADJUSTMENT', 'ACCOUNT_VIEW'), ('ADJUSTMENT', 'BOOKING_ADJUST'),
    ('ADJUSTMENT', 'WORK_VIEW'), ('ADJUSTMENT', 'ATTACHMENT_VIEW'),
    ('ADJUSTMENT', 'ATTACHMENT_MANAGE'), ('ADJUSTMENT', 'REPORT_VIEW'),
    -- Business Administrator (BRD 3.3)
    ('BUSINESS_ADMIN', 'LOV_MANAGE'), ('BUSINESS_ADMIN', 'ACCESS_REQUEST'),
    ('BUSINESS_ADMIN', 'AUDIT_VIEW'), ('BUSINESS_ADMIN', 'MASTER_VIEW'),
    ('BUSINESS_ADMIN', 'MASTER_MAINTAIN'), ('BUSINESS_ADMIN', 'MESSAGE_VIEW'),
    ('BUSINESS_ADMIN', 'WORK_VIEW'), ('BUSINESS_ADMIN', 'REPORT_VIEW'),
    -- Existing platform roles
    ('SYSADMIN', 'ACCESS_REQUEST'), ('SYSADMIN', 'MESSAGE_VIEW'), ('SYSADMIN', 'WORK_VIEW'),
    ('SYSADMIN', 'LOV_MANAGE'),
    ('FIN_MANAGER', 'CLIENT_VIEW'), ('FIN_MANAGER', 'QUOTE_VIEW'), ('FIN_MANAGER', 'ACCOUNT_VIEW'),
    ('FIN_MANAGER', 'WORK_VIEW'),
    ('AUDITOR', 'CLIENT_VIEW'), ('AUDITOR', 'QUOTE_VIEW'), ('AUDITOR', 'ACCOUNT_VIEW'),
    ('AUDITOR', 'WORK_VIEW'), ('AUDITOR', 'MESSAGE_VIEW')
) as g(role_code, permission) on g.role_code = r.code;

-- ---------- Lists of values (BRNB.083) ------------------------------------------------
create table lov_type (
    id           bigint generated by default as identity primary key,
    version      bigint       not null default 0,
    code         varchar(40)  not null unique,
    name         varchar(120) not null,
    description  varchar(300),
    maintainable boolean      not null default true,
    created_at   timestamptz  not null,
    created_by   varchar(50)  not null,
    updated_at   timestamptz,
    updated_by   varchar(50)
);

create table lov_value (
    id             bigint generated by default as identity primary key,
    version        bigint       not null default 0,
    type_code      varchar(40)  not null references lov_type (code),
    code           varchar(40)  not null,
    label          varchar(200) not null,
    sort_order     integer      not null default 0,
    parent_code    varchar(40),
    effective_from date         not null,
    effective_to   date,
    record_status  varchar(30)  not null,
    authorized_by  varchar(50),
    authorized_at  timestamptz,
    created_at     timestamptz  not null,
    created_by     varchar(50)  not null,
    updated_at     timestamptz,
    updated_by     varchar(50),
    constraint uq_lov_value unique (type_code, code),
    constraint ck_lov_value_dates check (effective_to is null or effective_to >= effective_from)
);
create index ix_lov_value_type on lov_value (type_code, sort_order);

-- Shared LOV types; each broking module seeds its own types in its own migration.
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('RETURN_REASON', 'Return reason', 'Why a record is returned to the previous team', true, now(), 'SYSTEM'),
       ('VOID_REASON', 'Void reason', 'Why an in-process record is voided (soft deleted)', true, now(), 'SYSTEM'),
       ('CANCELLATION_REASON', 'Cancellation reason', 'Why a placement or booked account is cancelled', true, now(), 'SYSTEM'),
       ('DOCUMENT_TYPE', 'Document type', 'Type of an uploaded client or account document', true, now(), 'SYSTEM'),
       ('MARKET_SEGMENT', 'Market segment', 'BDOI business segment of a client or account', true, now(), 'SYSTEM'),
       ('SOURCE_CHANNEL', 'Source channel', 'How a request reached BDOI', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from (values
    ('RETURN_REASON', 'INCOMPLETE_DETAILS', 'Incomplete or incorrect details', 10),
    ('RETURN_REASON', 'MISSING_DOCUMENTS', 'Missing supporting documents', 20),
    ('RETURN_REASON', 'INSURER_DECLINED', 'Declined by insurer', 30),
    ('RETURN_REASON', 'INSURER_REQUIREMENTS', 'Additional insurer requirements', 40),
    ('RETURN_REASON', 'RATE_REVIEW', 'Rate or terms to be reviewed', 50),
    ('RETURN_REASON', 'OTHERS', 'Others (see comment)', 90),
    ('VOID_REASON', 'DUPLICATE', 'Duplicate record', 10),
    ('VOID_REASON', 'ENCODING_ERROR', 'Encoding error', 20),
    ('VOID_REASON', 'CLIENT_WITHDREW', 'Client withdrew the request', 30),
    ('VOID_REASON', 'OTHERS', 'Others (see comment)', 90),
    ('CANCELLATION_REASON', 'CLIENT_REQUEST', 'Client request', 10),
    ('CANCELLATION_REASON', 'NON_PAYMENT', 'Non-payment of premium', 20),
    ('CANCELLATION_REASON', 'LOAN_CANCELLED', 'Loan cancelled or paid off', 30),
    ('CANCELLATION_REASON', 'INSURER_REQUEST', 'Insurer request', 40),
    ('CANCELLATION_REASON', 'OTHERS', 'Others (see comment)', 90),
    ('DOCUMENT_TYPE', 'IDF', 'Insurance Declaration Form (IDF)', 10),
    ('DOCUMENT_TYPE', 'VALID_ID', 'Valid ID', 20),
    ('DOCUMENT_TYPE', 'BIRTH_CERTIFICATE', 'Birth certificate', 30),
    ('DOCUMENT_TYPE', 'ACCOUNT_LIST', 'List of account details (Excel)', 40),
    ('DOCUMENT_TYPE', 'QUOTATION', 'Quotation / proposal', 50),
    ('DOCUMENT_TYPE', 'CLIENT_ACCEPTANCE', 'Client acceptance e-mail', 60),
    ('DOCUMENT_TYPE', 'EPOLICY', 'E-policy', 70),
    ('DOCUMENT_TYPE', 'OFFICIAL_RECEIPT', 'Official receipt', 80),
    ('DOCUMENT_TYPE', 'POLICY_COPY', 'Issued policy copy', 90),
    ('DOCUMENT_TYPE', 'OTHERS', 'Others', 99),
    ('MARKET_SEGMENT', 'CBG', 'Consumer Banking Group (CBG)', 10),
    ('MARKET_SEGMENT', 'COMBANK', 'Commercial Banking', 20),
    ('MARKET_SEGMENT', 'CORBANK', 'Corporate Banking', 30),
    ('MARKET_SEGMENT', 'RETAIL', 'Retail Marketing', 40),
    ('MARKET_SEGMENT', 'INSTITUTIONAL', 'Institutional Banking / SM / BDO accounts', 50),
    ('SOURCE_CHANNEL', 'EMAIL', 'E-mail', 10),
    ('SOURCE_CHANNEL', 'HLS', 'Home Loan System (HLS)', 20),
    ('SOURCE_CHANNEL', 'UPLOAD', 'Bulk upload', 30),
    ('SOURCE_CHANNEL', 'BRANCH_REFERRAL', 'Branch referral', 40),
    ('SOURCE_CHANNEL', 'WALK_IN', 'Walk-in / direct', 50)
) as v(type_code, code, label, sort_order);
