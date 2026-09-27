-- =====================================================================================
-- iNXT BrokerVerse - V780 Adjustment (BRD-2 Operations): endorsement and cancellation
-- requests on booked invoices, posting batches and the minimal balance write-off file.
--   Requirements: docs/requirements/BDOI_OPS_BRD_SPEC.md sections P, Q and R
--     ADJID.001-010  requests (financial, non-financial, internal), single or batch, return,
--                    approval, TSI increase / decrease, extension with additional premium
--     ADJID.011-014  posting through booking, payment re-application, AR Insurer, service invoice
--     ADJID.015-025  endorsement slip, reports, validation list and slip, ARN link, aging,
--                    history, duplicate check, supporting documents
--     ADJID.026/028  minimal balance write-off / credit file, over-adjustment control
--     MKTID.008      endorsement slip for Marketing
--   Design: docs/architecture/OPERATIONS_DESIGN.md sections 4.5, 5 (rows 16-21), 7 and 11.
--   Keys of other modules (invoice no., ARN, client, insurer) are plain values: no foreign keys
--   to the V8xx tables.
-- =====================================================================================

-- ---------- Return reasons (ADJID.005/007) -------------------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('ADJ_RETURN_REASON', 'Endorsement return reason',
        'Why an endorsement request is returned to the requester or taken out of a posting batch (ADJID.005/007)',
        true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'ADJ_RETURN_REASON', v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    ('NOT_QUALIFIED', 'Not qualified for posting', 10),
    ('INCOMPLETE_DOCUMENTS', 'Incomplete supporting documents', 20),
    ('INCORRECT_DETAILS', 'Incorrect request details', 30),
    ('INCORRECT_AMOUNTS', 'Incorrect amounts', 40),
    ('DUPLICATE', 'Duplicate request', 50),
    ('OTHERS', 'Others (see comment)', 90)
) as v(code, label, sort_order);

-- ---------- Workflow OPS_ENDORSEMENT (OPERATIONS_DESIGN section 7) --------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_ENDORSEMENT', 'DRAFT', 'Draft request', 'ADJ_REQUEST', null, true, false, 10),
       ('OPS_ENDORSEMENT', 'FOR_VALIDATION', 'For validation', 'ADJ_PROCESS', 24, false, false, 20),
       ('OPS_ENDORSEMENT', 'FOR_APPROVAL', 'For approval', 'ADJ_APPROVE', 24, false, false, 30),
       ('OPS_ENDORSEMENT', 'FOR_POSTING', 'For posting', 'ADJ_POST', 24, false, false, 40),
       ('OPS_ENDORSEMENT', 'AWAITING_REAPPLICATION', 'Posted - payments to re-apply', 'ADJ_POST', 48, false, false, 50),
       ('OPS_ENDORSEMENT', 'POSTED', 'Posted', null, null, false, true, 60),
       ('OPS_ENDORSEMENT', 'RETURNED', 'Returned to requester', 'ADJ_REQUEST', 48, false, false, 70),
       ('OPS_ENDORSEMENT', 'CANCELLED', 'Cancelled', null, null, false, true, 80);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_ENDORSEMENT', 'DRAFT', 'submit', 'FOR_VALIDATION', 'Submit for validation', 'ADJ_REQUEST,ADJ_PROCESS', false, null, 10),
       ('OPS_ENDORSEMENT', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel request', 'ADJ_REQUEST,ADJ_PROCESS', true, null, 90),
       ('OPS_ENDORSEMENT', 'FOR_VALIDATION', 'validate', 'FOR_APPROVAL', 'Validate for approval', 'ADJ_PROCESS', false, null, 10),
       ('OPS_ENDORSEMENT', 'FOR_VALIDATION', 'validate_for_posting', 'FOR_POSTING', 'Validate for posting', 'ADJ_PROCESS', false, null, 20),
       ('OPS_ENDORSEMENT', 'FOR_VALIDATION', 'return', 'RETURNED', 'Return to requester', 'ADJ_PROCESS', true, 'ADJ_RETURN_REASON', 80),
       ('OPS_ENDORSEMENT', 'FOR_APPROVAL', 'approve', 'FOR_POSTING', 'Approve', 'ADJ_APPROVE', false, null, 10),
       ('OPS_ENDORSEMENT', 'FOR_APPROVAL', 'return', 'RETURNED', 'Return to requester', 'ADJ_APPROVE', true, 'ADJ_RETURN_REASON', 80),
       ('OPS_ENDORSEMENT', 'FOR_POSTING', 'post', 'POSTED', 'Post', 'ADJ_POST', false, null, 10),
       ('OPS_ENDORSEMENT', 'FOR_POSTING', 'post_pending', 'AWAITING_REAPPLICATION', 'Post - payments to re-apply', 'ADJ_POST', false, null, 20),
       ('OPS_ENDORSEMENT', 'FOR_POSTING', 'return', 'RETURNED', 'Return to requester', 'ADJ_POST', true, 'ADJ_RETURN_REASON', 80),
       ('OPS_ENDORSEMENT', 'AWAITING_REAPPLICATION', 'reapply', 'POSTED', 'Re-apply payments', 'ADJ_POST', false, null, 10),
       ('OPS_ENDORSEMENT', 'RETURNED', 'resubmit', 'FOR_VALIDATION', 'Resubmit', 'ADJ_REQUEST,ADJ_PROCESS', false, null, 10),
       ('OPS_ENDORSEMENT', 'RETURNED', 'cancel', 'CANCELLED', 'Cancel request', 'ADJ_REQUEST,ADJ_PROCESS', true, null, 90);

-- ---------- Endorsement requests (OPERATIONS_DESIGN 4.5) -------------------------------------
create table adj_request (
    id                    bigint generated by default as identity primary key,
    version               bigint         not null default 0,
    company_id            bigint         not null references org_company (id),
    branch_id             bigint         not null references org_branch (id),
    request_no            varchar(40)    not null,
    request_class         varchar(20)    not null,
    computation           varchar(30)    not null,
    -- Invoice and account the request is raised on (ADJID.020: ARN kept on every request)
    invoice_no            varchar(40)    not null,
    arn                   varchar(30)    not null,
    policy_no             varchar(60),
    client_code           varchar(30)    not null,
    assured_name          varchar(250)   not null,
    insurer_code          varchar(30)    not null,
    currency              varchar(3)     not null,
    segment               varchar(40),
    ao_username           varchar(50),
    product_line          varchar(30),
    -- What is asked (ADJID.002/004, Annex V)
    endorsement_type      varchar(40)    not null,
    request_type          varchar(40),
    reason_code           varchar(40),
    endorsement_ref       varchar(60),
    effective_date        date           not null,
    refund_basis          varchar(20)    not null default 'PRO_RATA',
    sum_insured_change    numeric(19, 2),
    rate_percent          numeric(19, 8),
    new_period_from       date,
    new_period_to         date,
    description           varchar(1000)  not null,
    instructions          varchar(1000),
    -- Amounts entered for an amount change (signed)
    in_basic              numeric(19, 2),
    in_dst                numeric(19, 2),
    in_premium_tax_vat    numeric(19, 2),
    in_lgt                numeric(19, 2),
    in_fst                numeric(19, 2),
    in_other              numeric(19, 2),
    in_commission         numeric(19, 2),
    in_vat_on_commission  numeric(19, 2),
    -- Processing
    stage                 varchar(30)    not null,
    needs_approval        boolean        not null,
    negative              boolean        not null default false,
    duplicate_override    varchar(500),
    baseline_override     varchar(500),
    quotation_required    boolean        not null default false,
    quotation_ref         varchar(40),
    handoff_ref           varchar(40),
    return_reason         varchar(40),
    return_comment        varchar(1000),
    submitted_by          varchar(50),
    submitted_at          timestamptz,
    validated_by          varchar(50),
    validated_at          timestamptz,
    approved_by           varchar(50),
    approved_at           timestamptz,
    posted_by             varchar(50),
    posted_at             timestamptz,
    completed_at          timestamptz,
    -- Posting outcome (ADJID.011-014)
    batch_no              varchar(40),
    endorsement_no        varchar(40),
    new_invoice_no        varchar(40),
    service_invoices      varchar(500),
    ar_insurer_amount     numeric(19, 2),
    excess_amount         numeric(19, 2),
    unapplied_ref         varchar(60),
    slip_no               varchar(40),
    created_at            timestamptz    not null,
    created_by            varchar(50)    not null,
    updated_at            timestamptz,
    updated_by            varchar(50),
    constraint uq_adj_request_no unique (request_no),
    constraint ck_adj_request_class check (request_class in ('FINANCIAL', 'NON_FINANCIAL', 'INTERNAL')),
    constraint ck_adj_request_computation check (computation in ('NONE', 'CANCELLATION_FLAT',
        'CANCELLATION_FLAT_RETAIN_DST', 'CANCELLATION_PARTIAL', 'SUM_INSURED', 'AMOUNTS', 'WRITE_OFF')),
    constraint ck_adj_request_stage check (stage in ('DRAFT', 'FOR_VALIDATION', 'FOR_APPROVAL', 'FOR_POSTING',
        'AWAITING_REAPPLICATION', 'POSTED', 'RETURNED', 'CANCELLED')),
    constraint ck_adj_request_basis check (refund_basis in ('PRO_RATA', 'SHORT_PERIOD')),
    constraint ck_adj_request_period check (new_period_to is null or new_period_from is null
        or new_period_to > new_period_from)
);
create index ix_adj_request_invoice on adj_request (invoice_no);
create index ix_adj_request_stage on adj_request (company_id, stage);
create index ix_adj_request_arn on adj_request (arn);
create index ix_adj_request_new_invoice on adj_request (new_invoice_no);
create index ix_adj_request_batch on adj_request (batch_no);

-- Before / after per component (ADJID.014/022): after = before + delta.
create table adj_request_component (
    request_id    bigint         not null references adj_request (id),
    line_index    integer        not null,
    component     varchar(20)    not null,
    before_amount numeric(19, 2) not null,
    delta_amount  numeric(19, 2) not null,
    primary key (request_id, line_index)
);

-- Recompute per insurer share (ADJID.014/027).
create table adj_request_share (
    request_id       bigint         not null references adj_request (id),
    share_index      integer        not null,
    insurer_code     varchar(30)    not null,
    share_pct        numeric(9, 4)  not null,
    lead             boolean        not null,
    premium_delta    numeric(19, 2) not null,
    commission_delta numeric(19, 2) not null,
    vat_delta        numeric(19, 2) not null,
    primary key (request_id, share_index)
);

-- Journal batches posted for the request (ADJID.017 validation list).
create table adj_request_journal (
    request_id    bigint      not null references adj_request (id),
    journal_index integer     not null,
    batch_no      varchar(40) not null,
    primary key (request_id, journal_index)
);

-- ---------- Posting batches (ADJID.005/006, validation batch VB-<yyyy>) ----------------------
create table adj_posting_batch (
    id              bigint generated by default as identity primary key,
    version         bigint       not null default 0,
    company_id      bigint       not null references org_company (id),
    batch_no        varchar(40)  not null,
    posted_count    integer      not null default 0,
    pending_count   integer      not null default 0,
    failed_count    integer      not null default 0,
    remarks         varchar(500),
    created_at      timestamptz  not null,
    created_by      varchar(50)  not null,
    updated_at      timestamptz,
    updated_by      varchar(50),
    constraint uq_adj_posting_batch unique (batch_no)
);

create table adj_posting_batch_line (
    batch_id    bigint        not null references adj_posting_batch (id),
    line_index  integer       not null,
    request_id  bigint        not null references adj_request (id),
    request_no  varchar(40)   not null,
    invoice_no  varchar(40)   not null,
    outcome     varchar(30)   not null,
    message     varchar(1000),
    primary key (batch_id, line_index),
    constraint ck_adj_batch_line_outcome check (outcome in ('POSTED', 'AWAITING_REAPPLICATION', 'FAILED'))
);

-- ---------- Minimal balance write-off / credit file (ADJID.026) ------------------------------
create table adj_min_balance_item (
    id               bigint generated by default as identity primary key,
    version          bigint         not null default 0,
    company_id       bigint         not null references org_company (id),
    file_ref         varchar(40)    not null,
    invoice_no       varchar(40)    not null,
    arn              varchar(30)    not null,
    client_code      varchar(30)    not null,
    currency         varchar(3)     not null,
    balance          numeric(19, 2) not null,
    action           varchar(20)    not null,
    journal_batch_no varchar(40),
    created_at       timestamptz    not null,
    created_by       varchar(50)    not null,
    updated_at       timestamptz,
    updated_by       varchar(50),
    constraint uq_adj_min_balance_invoice unique (invoice_no),
    constraint ck_adj_min_balance_action check (action in ('WRITE_OFF', 'CREDIT'))
);
create index ix_adj_min_balance_file on adj_min_balance_item (file_ref);

-- ---------- Accounting events (OPERATIONS_DESIGN section 5; rules by Comptrollership, OQ07) --
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('OPS_AR_INSURER_SETUP', 'AR Insurer set-up after a remitted decrease', 'PREMIUM', 'PREMIUM',
  'Cancellation or decrease of an invoice already remitted (row 18): the return premium already paid to '
  || 'the insurer is reclassified from due to insurer to AR Insurer (party = insurer).',
  'AR_INSURER'),
 ('OPS_WRITE_OFF', 'Minimal balance write-off / credit', 'PREMIUM', 'PREMIUM',
  'Minimal balance file 10.00-100.00 (row 21, ADJID.026): a debit balance written off by premium '
  || 'receivable component, or a credit balance taken to income.',
  'WRITE_OFF,PR_BASIC,PR_DST,PR_PTX_VAT,PR_LGT,PR_FST,PR_OTHER,CREDIT_BALANCE'),
 ('OPS_ADJ_COMMISSION', 'Commission adjustment without premium change', 'COMMISSION', 'COMMISSION',
  'Increase or decrease of the commission of a booked invoice without premium change (ADJID.014): '
  || 'commission receivable against unrealized commission and deferred output VAT; negative amounts reverse.',
  'COMMISSION_RECEIVABLE,UNREALIZED_COMMISSION,DEFERRED_OUTPUT_VAT');

-- ---------- Business parameters -------------------------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('ADJ_BASELINE_PERCENT', '100', 'DECIMAL', 'OPERATIONS',
     'Over-adjustment baseline: cumulative adjustments of an invoice above this percent of its original premium need an override reason and raise ADJ_OVER_BASELINE (ADJID.028, OQ37)',
     1, 1000, now(), 'SYSTEM');

-- ---------- Document templates (ADJID.015/018, MKTID.008) ----------------------------------
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('ENDORSEMENT_SLIP', 1, 'Endorsement slip',
 'Please endorse policy {{policyNo}} of {{assured}} as requested below under our endorsement request {{requestNo}}, effective {{effectiveDate}}. Kindly send the endorsement to BDOI quoting the invoice {{invoiceNo}} and our Account Reference Number {{arn}}.', date '2020-01-01', now(), 'SYSTEM'),
('VALIDATION_SLIP', 1, 'Validation slip',
 'Endorsement request {{requestNo}} on invoice {{invoiceNo}} was validated on {{validatedOn}} and is {{status}}. The accounting entries and the insurer breakdown are shown below.', date '2020-01-01', now(), 'SYSTEM');
