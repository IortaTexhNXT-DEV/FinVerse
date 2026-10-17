-- =====================================================================================
-- iNXT BrokerVerse - V861 Issuance: extraction patterns per document kind (contract change of
-- Submitted Policies wave S0, in the issuance range V860-V869).
--   Requirements: docs/requirements/BDOI_SP_BRD_SPEC.md BRIDSP-02 (policy details extracted from
--                 the submitted documents and saved after the user's confirmation).
--   Design: docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 2.2 and 9 (issuance row).
--   * kind EPOLICY = e-policies of BIBS accounts (the existing patterns); SUBMITTED_POLICY =
--     policies bought elsewhere and submitted to the bank.
--   * The fields of a submitted policy (assured, PN, insurer, sum insured, unit, serial, motor and
--     plate numbers, location) join the field check. The default patterns below read the common
--     labels of a policy schedule; insurer patterns are added by the administrator.
-- =====================================================================================

alter table iss_extraction_pattern add column kind varchar(20) not null default 'EPOLICY';
alter table iss_extraction_pattern add constraint ck_iss_pattern_kind check (kind in ('EPOLICY', 'SUBMITTED_POLICY'));
alter table iss_extraction_pattern drop constraint if exists ck_iss_pattern_field;
alter table iss_extraction_pattern add constraint ck_iss_pattern_field
    check (field in ('POLICY_NUMBER', 'PERIOD_FROM', 'PERIOD_TO', 'PREMIUM', 'ASSURED', 'PN_NO', 'INSURER',
                     'SUM_INSURED', 'UNIT', 'SERIAL_NO', 'MOTOR_NO', 'PLATE_NO', 'LOCATION'));
drop index if exists ix_iss_pattern_insurer;
create index ix_iss_pattern_insurer on iss_extraction_pattern (kind, insurer_code, field, priority);

insert into iss_extraction_pattern (insurer_code, field, pattern, date_format, priority, kind, created_at, created_by)
values
 (null, 'POLICY_NUMBER', '(?i)policy\s*(?:no\.?|number|#)\s*[:#]?\s*([A-Z0-9][A-Z0-9/-]{3,39})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'ASSURED', '(?im)(?:name\s+of\s+)?assured\s*:\s*([^\r\n]{2,120})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'PN_NO', '(?i)(?:PN|promissory\s+note)\s*(?:no\.?|number|#)?\s*[:#]?\s*([A-Z0-9][A-Z0-9-]{3,39})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'INSURER', '(?im)(?:insurer|insurance\s+company)\s*:\s*([^\r\n]{2,120})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'PERIOD_FROM', '(?i)period\s+of\s+insurance\s*:?\s*(?:from\s+)?(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})', 'MM/dd/yyyy', 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'PERIOD_TO', '(?i)period\s+of\s+insurance\s*:?\s*(?:from\s+)?(?:\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})\s*(?:to|-)\s*(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})', 'MM/dd/yyyy', 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'SUM_INSURED', '(?i)(?:sum|amount)\s+insured\s*:?\s*(?:PHP|P)?\s*([0-9][0-9,]*(?:\.[0-9]{2})?)', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'PREMIUM', '(?i)(?:total|gross)\s+premium\s*:?\s*(?:PHP|P)?\s*([0-9][0-9,]*\.[0-9]{2})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'UNIT', '(?im)(?:unit|vehicle)\s*(?:description)?\s*:\s*([^\r\n]{2,120})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'SERIAL_NO', '(?i)(?:serial|chassis)\s*(?:no\.?|number)\s*[:#]?\s*([A-Z0-9-]{4,40})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'MOTOR_NO', '(?i)(?:motor|engine)\s*(?:no\.?|number)\s*[:#]?\s*([A-Z0-9-]{4,40})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'PLATE_NO', '(?i)plate\s*(?:no\.?|number)\s*[:#]?\s*([A-Z0-9 -]{3,20})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM'),
 (null, 'LOCATION', '(?im)(?:location\s+of\s+risk|property\s+location)\s*:\s*([^\r\n]{2,200})', null, 10, 'SUBMITTED_POLICY', now(), 'SYSTEM');
