-- =====================================================================================
-- iNXT BrokerVerse - V981 Seed CRM clients (SEED DATA ONLY - never load in production).
--   12 clients: confirmed and prospects, individuals and corporates, BDO bank and non-bank,
--   tags and special instructions, one client with an overdue KYC review (BRNB.110), one
--   inactive client and one dormant prospect for the retention review (BRNB.106).
--   Confirmed clients carry an authorized sub-ledger party whose code is the client code.
--   Codes PR-2026-000001..012 / CL-2026-000001..006; the number series continue at 101.
-- =====================================================================================

create temporary table seed_client (
    prospect_code varchar(30), client_code varchar(30), status varchar(20), client_type varchar(20),
    last_name varchar(100), first_name varchar(100), middle_name varchar(100), corporate_name varchar(200),
    birth_date date, tin varchar(20), id_type varchar(40), id_number varchar(60), email varchar(120),
    mobile varchar(30), address_line varchar(300), city varchar(80), province varchar(80),
    postal_code varchar(10), market_segment varchar(40), bank_client boolean, bank_cif varchar(40),
    nationality varchar(40), civil_status varchar(40), occupation varchar(120),
    source_of_funds varchar(40), risk_rating varchar(40), stage varchar(40), kyc_status varchar(20),
    review_days integer, created timestamptz
) on commit drop;

insert into seed_client values
('PR-2026-000001', 'CL-2026-000001', 'CONFIRMED', 'INDIVIDUAL', 'Santos', 'Maria Clara', 'Reyes', null,
 date '1984-03-12', '201-555-101-000', 'PASSPORT', 'P5551010A', 'maria.santos@seed-client.ph', '09175550101',
 '21 Paseo de Roxas', 'Makati', 'Metro Manila', '1226', 'CBG', true, 'CIF-00550101',
 'FILIPINO', 'MARRIED', 'Physician', 'SALARY', 'STANDARD', 'CONFIRMED', 'VERIFIED', 900, now()),
('PR-2026-000002', 'CL-2026-000002', 'CONFIRMED', 'INDIVIDUAL', 'Reyes', 'Jose Miguel', 'Lopez', null,
 date '1979-11-02', '201-555-102-000', 'DRIVERS_LICENSE', 'N01-55-102030', 'jm.reyes@seed-client.ph', '09185550102',
 '8 Katipunan Ave', 'Quezon City', 'Metro Manila', '1108', 'RETAIL', false, null,
 'FILIPINO', 'SINGLE', 'Business owner', 'BUSINESS', 'HIGH', 'CONFIRMED', 'EXPIRED', -30, now()),
('PR-2026-000003', 'CL-2026-000003', 'CONFIRMED', 'CORPORATE', null, null, null, 'Pacific Harbor Logistics Inc.',
 null, '301-555-201-000', 'SEC_REG', 'CS201955501', 'treasury@pacificharbor.example', '09175550201',
 '3 Harbor Drive, Port Area', 'Manila', 'Metro Manila', '1018', 'CORBANK', true, 'CIF-00550201',
 null, null, 'Freight forwarding', 'BUSINESS', 'STANDARD', 'CONFIRMED', 'VERIFIED', 1000, now()),
('PR-2026-000004', 'CL-2026-000004', 'CONFIRMED', 'CORPORATE', null, null, null, 'Luzon Agri-Industrial Corp.',
 null, '301-555-202-000', 'SEC_REG', 'CS201955502', 'finance@luzonagri.example', '09175550202',
 'Km 52 National Highway', 'Santo Tomas', 'Batangas', '4234', 'COMBANK', false, null,
 null, null, 'Feed milling', 'BUSINESS', 'STANDARD', 'CONFIRMED', 'VERIFIED', 20, now()),
('PR-2026-000005', 'CL-2026-000005', 'CONFIRMED', 'INDIVIDUAL', 'Garcia', 'Antonio Luis', 'Dizon', null,
 date '1990-07-21', '201-555-105-000', 'UMID', '0111-5550105-3', 'antonio.garcia@seed-client.ph', '09175550105',
 '45 Ortigas Ave', 'Pasig', 'Metro Manila', '1605', 'CBG', true, 'CIF-00550105',
 'FILIPINO', 'MARRIED', 'Bank officer', 'SALARY', 'LOW', 'CONFIRMED', 'VERIFIED', 1080, now()),
('PR-2026-000006', 'CL-2026-000006', 'CONFIRMED', 'INDIVIDUAL', 'Villanueva', 'Carmela Isabel', 'Santos', null,
 date '1972-01-30', '201-555-106-000', 'PHILSYS', '5555-0106-2026-0001', 'carmela.v@seed-client.ph', '09185550106',
 '12 Lahug Road', 'Cebu City', 'Cebu', '6000', 'RETAIL', false, null,
 'FILIPINO', 'WIDOWED', 'Retired teacher', 'PENSION', 'STANDARD', 'CONFIRMED', 'VERIFIED', 700, now()),
('PR-2026-000007', null, 'PROSPECT', 'INDIVIDUAL', 'Mendoza', 'Rafael Jose', 'Castro', null,
 date '1988-05-05', '201-555-107-000', 'PASSPORT', 'P5551070B', 'rafael.mendoza@seed-client.ph', '09175550107',
 '77 Shaw Blvd', 'Mandaluyong', 'Metro Manila', '1552', 'CBG', true, 'CIF-00550107',
 'FILIPINO', 'SINGLE', 'Engineer', 'SALARY', 'STANDARD', 'KYC_REVIEW', 'PENDING', null, now()),
('PR-2026-000008', null, 'PROSPECT', 'CORPORATE', null, null, null, 'Bayside Builders Co.',
 null, '301-555-208-000', 'SEC_REG', 'CS201955508', 'admin@baysidebuilders.example', '09175550208',
 '9 Coastal Road', 'Paranaque', 'Metro Manila', '1700', 'COMBANK', false, null,
 null, null, 'General construction', 'BUSINESS', 'STANDARD', 'KYC_VERIFIED', 'VERIFIED', 1095, now()),
('PR-2026-000009', null, 'PROSPECT', 'INDIVIDUAL', 'Aquino', 'Lorna Faye', null, null,
 null, null, null, null, null, '09175550109',
 null, null, null, null, 'CBG', false, null,
 null, null, null, null, null, 'PROSPECT', 'NOT_STARTED', null, now()),
('PR-2026-000010', null, 'PROSPECT', 'CORPORATE', null, null, null, 'Metro Dental Clinic Partners',
 null, '301-555-210-000', null, null, 'clinic@metrodental.example', null,
 '101 Aurora Blvd', 'San Juan', 'Metro Manila', '1500', 'RETAIL', false, null,
 null, null, 'Dental clinic', 'BUSINESS', null, 'PROSPECT', 'NOT_STARTED', null, now()),
('PR-2026-000011', null, 'PROSPECT', 'INDIVIDUAL', 'Cruz', 'Benjamin Tomas', 'Ramos', null,
 date '1965-09-09', '201-555-111-000', 'SSS', '33-5550111-1', 'ben.cruz@seed-client.ph', '09175550111',
 '5 Session Road', 'Baguio', 'Benguet', '2600', 'CBG', true, 'CIF-00550111',
 'FILIPINO', 'MARRIED', 'Farm owner', 'BUSINESS', 'STANDARD', 'PROSPECT', 'NOT_STARTED', null,
 timestamptz '2019-03-01 09:00:00+08'),
('PR-2026-000012', null, 'INACTIVE', 'INDIVIDUAL', 'Lim', 'Stephanie Ann', 'Go', null,
 date '1993-12-24', '201-555-112-000', 'PASSPORT', 'P5551120C', 'stephanie.lim@seed-client.ph', '09175550112',
 '18 Binondo St', 'Manila', 'Metro Manila', '1006', 'RETAIL', false, null,
 'FILIPINO', 'SINGLE', 'Accountant', 'SALARY', 'STANDARD', 'INACTIVE', 'NOT_STARTED', null,
 timestamptz '2020-01-15 09:00:00+08');

insert into crm_client (company_id, prospect_code, client_code, status, client_type, last_name, first_name,
    middle_name, corporate_name, display_name, birth_date, tin, id_type, id_number, email, mobile,
    address_line, city, province, postal_code, market_segment, bank_client, bank_cif, party_code,
    kyc_status, kyc_submitted_by, kyc_submitted_at, kyc_verified_by, kyc_verified_at, kyc_review_due,
    confirmed_by, confirmed_at, nationality, civil_status, occupation, source_of_funds, risk_rating,
    onboarding_stage, deactivation_reason, deactivation_note, deactivated_by, deactivated_at,
    id_key, mobile_key, name_key, corporate_key, created_at, created_by, updated_at, updated_by)
select c.id, d.prospect_code, d.client_code, d.status, d.client_type, d.last_name, d.first_name,
       d.middle_name, d.corporate_name,
       case when d.client_type = 'CORPORATE' then d.corporate_name
            else d.last_name || ', ' || d.first_name || coalesce(' ' || d.middle_name, '') end,
       d.birth_date, d.tin, d.id_type, d.id_number, d.email, d.mobile,
       d.address_line, d.city, d.province, d.postal_code, d.market_segment, d.bank_client, d.bank_cif,
       d.client_code,
       d.kyc_status,
       case when d.stage <> 'PROSPECT' and d.stage <> 'INACTIVE' then 'ao' end,
       case when d.stage <> 'PROSPECT' and d.stage <> 'INACTIVE' then now() - interval '20 days' end,
       case when d.kyc_status in ('VERIFIED', 'EXPIRED') then 'mkttl' end,
       case when d.kyc_status in ('VERIFIED', 'EXPIRED') then now() - interval '15 days' end,
       case when d.review_days is not null then current_date + d.review_days end,
       case when d.status = 'CONFIRMED' then 'mkttl' end,
       case when d.status = 'CONFIRMED' then now() - interval '14 days' end,
       d.nationality, d.civil_status, d.occupation, d.source_of_funds, d.risk_rating,
       d.stage,
       case when d.status = 'INACTIVE' then 'DUPLICATE' end,
       case when d.status = 'INACTIVE' then 'Duplicate of an existing client record' end,
       case when d.status = 'INACTIVE' then 'ao' end,
       case when d.status = 'INACTIVE' then d.created end,
       case when d.id_type is not null and d.id_number is not null
            then upper(d.id_type) || ':' || upper(regexp_replace(d.id_number, '[\s-]', '', 'g')) end,
       d.mobile,
       case when d.last_name is not null
            then lower(regexp_replace(d.last_name, '\s+', '', 'g')) || '|' || lower(regexp_replace(d.first_name, '\s+', '', 'g')) end,
       case d.corporate_name
            when 'Pacific Harbor Logistics Inc.' then 'PACIFICHARBORLOGISTICS'
            when 'Luzon Agri-Industrial Corp.' then 'LUZONAGRIINDUSTRIAL'
            when 'Bayside Builders Co.' then 'BAYSIDEBUILDERS'
            when 'Metro Dental Clinic Partners' then 'METRODENTALCLINICPARTNERS' end,
       d.created, 'ao', d.created, 'ao'
from seed_client d
join org_company c on c.code = 'FVI';

-- Sub-ledger parties of the confirmed clients (code = client code), authorized (V901 pattern).
insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, authorized_by, authorized_at, created_at, created_by)
select c.company_id, c.client_code, c.display_name,
       case c.client_type when 'CORPORATE' then 'CORPORATE_CLIENT' else 'INDIVIDUAL_CLIENT' end,
       c.tin, c.address_line || ', ' || c.city, c.email, c.mobile, 'PHP', 30,
       'ACTIVE', 'SYSTEM', now(), now(), 'mkttl'
from crm_client c
where c.client_code like 'CL-2026-0000%' and c.created_by = 'ao';

-- Onboarding work cases (workflow NB_CLIENT) with their status history.
insert into wf_case (company_id, workflow_code, entity_type, entity_id, reference, title, link,
    originating_unit, stage_code, stage_entered_at, due_at, assignee, closed, created_at, created_by)
select c.company_id, 'NB_CLIENT', 'Client', c.id::text, coalesce(c.client_code, c.prospect_code),
       c.display_name, '/crm/clients/' || c.id, c.market_segment, c.onboarding_stage,
       now() - interval '2 days',
       case c.onboarding_stage when 'PROSPECT' then now() + interval '70 hours'
                               when 'KYC_REVIEW' then now() + interval '22 hours'
                               when 'KYC_VERIFIED' then now() + interval '22 hours' end,
       case when c.onboarding_stage in ('PROSPECT', 'KYC_VERIFIED') then 'ao' end,
       c.onboarding_stage = 'INACTIVE', c.created_at, 'ao'
from crm_client c
join seed_client d on d.prospect_code = c.prospect_code;

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor,
    automatic, occurred_at)
select w.id, null, 'PROSPECT', 'start', null, null, 'ao', false, w.created_at
from wf_case w
where w.workflow_code = 'NB_CLIENT' and w.created_by = 'ao'
  and w.reference in (select coalesce(client_code, prospect_code) from seed_client);

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor,
    automatic, occurred_at)
select w.id, 'PROSPECT', w.stage_code,
       case w.stage_code when 'KYC_REVIEW' then 'submit_kyc' when 'KYC_VERIFIED' then 'verify_kyc'
                         when 'CONFIRMED' then 'confirm' else 'deactivate' end,
       case w.stage_code when 'INACTIVE' then 'DUPLICATE' end,
       'Seed onboarding history',
       case w.stage_code when 'KYC_REVIEW' then 'ao' when 'INACTIVE' then 'ao' else 'mkttl' end,
       false, now() - interval '2 days'
from wf_case w
where w.workflow_code = 'NB_CLIENT' and w.created_by = 'ao' and w.stage_code <> 'PROSPECT'
  and w.reference in (select coalesce(client_code, prospect_code) from seed_client);

-- KYC documents (small PDF placeholders) of the clients past the prospect stage.
insert into doc_attachment (entity_type, entity_id, file_name, content_type, size_bytes, sha256,
    description, created_at, created_by)
select 'Client', c.id::text, lower(v.code) || '_' || c.prospect_code || '.pdf', 'application/pdf',
       octet_length(b.content), encode(sha256(b.content), 'hex'), 'KYC: ' || v.label,
       now() - interval '21 days', 'ao'
from crm_client c
join seed_client d on d.prospect_code = c.prospect_code
join lov_value v on v.type_code = case c.client_type when 'CORPORATE' then 'KYC_DOCS_CORPORATE'
                                                     else 'KYC_DOCS_INDIVIDUAL' end
cross join (select convert_to('%PDF-1.4' || chr(10) || '% BrokerVerse seed KYC document' || chr(10) || '%%EOF', 'UTF8') as content) b
where d.stage in ('KYC_REVIEW', 'KYC_VERIFIED', 'CONFIRMED');

insert into doc_attachment_content (attachment_id, content)
select a.id, convert_to('%PDF-1.4' || chr(10) || '% BrokerVerse seed KYC document' || chr(10) || '%%EOF', 'UTF8')
from doc_attachment a
where a.entity_type = 'Client' and a.description like 'KYC: %'
  and a.file_name like '%\_PR-2026-0000%.pdf'
  and not exists (select 1 from doc_attachment_content x where x.attachment_id = a.id);

insert into crm_kyc_document (client_id, document_type, attachment_id, created_at, created_by)
select c.id, v.code, a.id, a.created_at, 'ao'
from crm_client c
join seed_client d on d.prospect_code = c.prospect_code
join lov_value v on v.type_code = case c.client_type when 'CORPORATE' then 'KYC_DOCS_CORPORATE'
                                                     else 'KYC_DOCS_INDIVIDUAL' end
join doc_attachment a on a.entity_type = 'Client' and a.entity_id = c.id::text
                     and a.file_name = lower(v.code) || '_' || c.prospect_code || '.pdf';

-- Tags and special instructions (BRNB.091).
insert into crm_client_tag (client_id, tag_code, active, created_at, created_by)
select c.id, t.tag, true, now() - interval '10 days', 'ao'
from crm_client c
join (values ('PR-2026-000003', 'VIP'), ('PR-2026-000001', 'VIP'), ('PR-2026-000005', 'BANK_EMPLOYEE'),
             ('PR-2026-000006', 'DO_NOT_CALL'), ('PR-2026-000002', 'WATCHLIST_REVIEW')) as t(code, tag)
  on t.code = c.prospect_code;

insert into crm_client_instruction (client_id, instruction_type, instruction_text, effective_from,
    effective_to, active, created_at, created_by)
select c.id, i.type, i.text, current_date - 30, null, true, now() - interval '10 days', 'ao'
from crm_client c
join (values
    ('PR-2026-000001', 'COMMUNICATION', 'Send policy documents by e-mail only; no courier deliveries.'),
    ('PR-2026-000003', 'BILLING', 'Bill quarterly; send the statement of account to treasury@pacificharbor.example.'),
    ('PR-2026-000006', 'COMMUNICATION', 'Client asked not to be called; contact by e-mail or letter.')
) as i(code, type, text) on i.code = c.prospect_code;

insert into crm_client_note_history (client_id, item_kind, item_ref, action, from_value, to_value,
    actor, occurred_at)
select t.client_id, 'TAG', t.tag_code, 'ADDED', null, v.label, 'ao', t.created_at
from crm_client_tag t
join lov_value v on v.type_code = 'CLIENT_TAG' and v.code = t.tag_code
where t.created_by = 'ao';

insert into crm_client_note_history (client_id, item_kind, item_ref, action, from_value, to_value,
    actor, occurred_at)
select i.client_id, 'INSTRUCTION', i.id::text, 'ADDED', null,
       i.instruction_type || ': ' || i.instruction_text || ' (' || i.effective_from || ' onwards)',
       'ao', i.created_at
from crm_client_instruction i
where i.created_by = 'ao';

-- Number series continue after the seed codes.
insert into document_sequence (sequence_key, next_value) values ('PR-2026', 101), ('CL-2026', 101)
on conflict (sequence_key) do update set next_value = greatest(document_sequence.next_value, 101);
