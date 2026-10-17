-- Claims, BRD-scope delivery work of the FRS BRD-07 Claims v1.0 (DW-CLM-01 to 13). The change requests CR-CLM-101 to
-- 112 of BDOI's FRS are not part of it (awaiting BDOI's approval).

-- DW-CLM-03: supervising Team Lead and Team Head of each handler in the handler register (FRCLM.002.06).
alter table bcl_handler add column team_lead varchar(50);
alter table bcl_handler add column team_head varchar(50);

-- DW-CLM-04: status type of each claim status (FRCLM.003.02), and every status list in alphabetical order.
insert into lov_type (code, name, description, maintainable, owner_permission, created_at, created_by)
values ('BCL_STATUS_TYPE', 'Claim status type', 'Type of a claim status: New PLA, Follow Up, Settlement, Settlement - Paid, Settlement - LOA Issued', true, 'BCL_SETUP', now(), 'SYSTEM'),
       ('BCL_FOLLOW_UP_TYPE', 'Follow-up type', 'Kind of a follow-up recorded on a claim', true, 'BCL_SETUP', now(), 'SYSTEM')
on conflict (code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from (values
    ('BCL_STATUS_TYPE', 'NEW_PLA', 'New PLA', 10),
    ('BCL_STATUS_TYPE', 'FOLLOW_UP', 'Follow Up', 20),
    ('BCL_STATUS_TYPE', 'SETTLEMENT', 'Settlement', 30),
    ('BCL_STATUS_TYPE', 'SETTLEMENT_PAID', 'Settlement - Paid', 40),
    ('BCL_STATUS_TYPE', 'SETTLEMENT_LOA', 'Settlement - LOA Issued', 50),
    -- DW-CLM-13 (G-CLM-03): follow-up types.
    ('BCL_FOLLOW_UP_TYPE', 'ADVISE_INSURER', 'Advise Insurer of Claim', 10),
    ('BCL_FOLLOW_UP_TYPE', 'APPOINT_ASSESSOR', 'Appoint Assessor', 20),
    ('BCL_FOLLOW_UP_TYPE', 'CHASE_SETTLEMENT_OFFER', 'Chase Insurer for Settlement Offer', 30),
    ('BCL_FOLLOW_UP_TYPE', 'FOLLOW_UP_CLAIMS', 'Claim Follow-Up - Claims', 40),
    ('BCL_FOLLOW_UP_TYPE', 'CLIENT_CLAIM_REPORT', 'Claim Report from Client', 50),
    ('BCL_FOLLOW_UP_TYPE', 'FOLLOW_UP_RECOVERY', 'Claims - Follow up Recovery', 60),
    ('BCL_FOLLOW_UP_TYPE', 'FOLLOW_UP_AE', 'Claims Follow-Up - AE', 70),
    ('BCL_FOLLOW_UP_TYPE', 'OBTAIN_POLICE_REPORT', 'Obtain Police Report', 80),
    -- DW-CLM-05: claim type codes of the core system (FRCLM.005.01); the types of claim of the category list follow
    -- with the data migration.
    ('BCL_CLAIM_TYPE', 'CG', 'Comprehensive General Liability', 100),
    ('BCL_CLAIM_TYPE', 'FL', 'Fire & Lightning', 110),
    ('BCL_CLAIM_TYPE', 'HP', 'Hospitalization', 120),
    ('BCL_CLAIM_TYPE', 'LC', 'Credit Card Loss/Fraud', 130),
    ('BCL_CLAIM_TYPE', 'LF', 'Life/Accident/Health', 140),
    ('BCL_CLAIM_TYPE', 'MC', 'Motor', 150),
    ('BCL_CLAIM_TYPE', 'MR', 'Marine Cover', 160),
    ('BCL_CLAIM_TYPE', 'OT', 'Bonds/BBB/Others/Miscellaneous', 170),
    -- DW-CLM-13 (G-CLM-01): claim documents of the standard checklist.
    ('DOCUMENT_TYPE', 'CLAIM_POLICE_REPORT', 'Police report', 140),
    ('DOCUMENT_TYPE', 'CLAIM_DRIVERS_AFFIDAVIT', 'Driver''s affidavit', 141),
    ('DOCUMENT_TYPE', 'CLAIM_DRIVERS_LICENSE', 'Driver''s license', 142),
    ('DOCUMENT_TYPE', 'CLAIM_DAMAGE_PHOTOS', 'Photos of the damage', 143),
    ('DOCUMENT_TYPE', 'CLAIM_FORM', 'Claim form signed by the claimant', 144),
    ('DOCUMENT_TYPE', 'CLAIM_MEDICAL_RECORDS', 'Medical records and bills', 145)
) as v(type_code, code, label, sort_order)
on conflict (type_code, code) do nothing;

alter table bcl_lov_attribute drop constraint ck_bcl_lov_attribute_name;
alter table bcl_lov_attribute add constraint ck_bcl_lov_attribute_name check (attribute in ('phase', 'waiting_on',
    'follow_up_days', 'awaiting_premium_remittance', 'outcome', 'closes_claim', 'requires_settlement_amount',
    'status_type'));

insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select 'BCL_CLAIM_STATUS', s.code, 'status_type', s.status_type, now(), 'SYSTEM'
from (values
    ('NEW_COMPLETE_DOCS', 'NEW_PLA'), ('NEW_INCOMPLETE_DOCS', 'NEW_PLA'),
    ('INSURER_CHECK_ISSUANCE', 'SETTLEMENT'), ('INSURER_RELEASE_PAPERS', 'SETTLEMENT'),
    ('BDOI_CHECK_TRANSMITTAL', 'SETTLEMENT_PAID'), ('INSURER_LOA_ISSUANCE', 'SETTLEMENT_LOA')
) as s(code, status_type)
where not exists (select 1 from bcl_lov_attribute a where a.type_code = 'BCL_CLAIM_STATUS' and a.code = s.code
                  and a.attribute = 'status_type');

insert into bcl_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select 'BCL_CLAIM_STATUS', v.code, 'status_type', 'FOLLOW_UP', now(), 'SYSTEM'
from lov_value v
where v.type_code = 'BCL_CLAIM_STATUS'
  and not exists (select 1 from bcl_lov_attribute a where a.type_code = 'BCL_CLAIM_STATUS' and a.code = v.code
                  and a.attribute = 'status_type');

update lov_value v
set sort_order = r.rank * 10
from (select id, row_number() over (order by label) as rank from lov_value where type_code = 'BCL_CLAIM_STATUS') r
where v.id = r.id;

-- DW-CLM-06, 07, 09, 13: plate number of a motor claim, preferred repair shop (a party of type GARAGE, FRCLM.007.02)
-- and the return of the claim of an unpaid policy to the Account Officer (FRCLM.008.04).
alter table bcl_claim add column plate_no varchar(20);
alter table bcl_claim add column preferred_shop_code varchar(30);
alter table bcl_claim add column returned_to_ao_at timestamptz;
alter table bcl_claim add column returned_to_ao_by varchar(50);
alter table bcl_claim add column returned_to_ao_note varchar(500);

-- DW-CLM-12: barangay of a claim location (FRCLM.017.01).
alter table bcl_claim_location add column barangay varchar(80);

-- DW-CLM-09 and 10: follow-ups with the fields of the BRD scope (FRCLM.009.01, 009.04).
create table bcl_follow_up (
    id                    bigint generated by default as identity primary key,
    version               bigint         not null default 0,
    claim_id              bigint         not null references bcl_claim (id),
    follow_up_type        varchar(40)    not null,
    action_taken          varchar(2000)  not null,
    next_plan_summary     varchar(500),
    next_plan_detail      varchar(2000),
    status_code           varchar(40)    not null,
    settlement_amount     numeric(19, 2),
    adjuster_evaluation   numeric(19, 2),
    insurer_deductible    numeric(19, 2),
    followed_up_on        date           not null,
    created_at            timestamptz    not null,
    created_by            varchar(50)    not null,
    updated_at            timestamptz,
    updated_by            varchar(50)
);
create index ix_bcl_follow_up_claim on bcl_follow_up (claim_id, followed_up_on);

-- DW-CLM-13 (G-CLM-01): documents required per claim type before a claim is filed with complete documents
-- (FRCLM.006.01, 006.02).
create table bcl_document_requirement (
    id                    bigint generated by default as identity primary key,
    version               bigint         not null default 0,
    claim_type            varchar(40)    not null,
    document_type         varchar(40)    not null,
    required              boolean        not null,
    active                boolean        not null default true,
    created_at            timestamptz    not null,
    created_by            varchar(50)    not null,
    updated_at            timestamptz,
    updated_by            varchar(50),
    constraint uq_bcl_document_requirement unique (claim_type, document_type)
);

-- Timeline entries of the new details: motor details, return to the Account Officer and follow-ups.
alter table bcl_claim_event drop constraint ck_bcl_claim_event_field;
alter table bcl_claim_event add constraint ck_bcl_claim_event_field check (field in ('REPORTED_DATE', 'CLAIMANT',
    'ADJUSTER', 'FOLLOW_UP', 'ACTION_PLAN', 'SETTLEMENT', 'COVER_VERSION', 'AUTHORIZATION', 'HANDLER', 'MOTOR',
    'RETURNED_TO_AO', 'FOLLOW_UP_RECORDED'));
