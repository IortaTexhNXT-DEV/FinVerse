-- =====================================================================================
-- iNXT BrokerVerse - V1951 Demo Sanction Screening (BRD-10) watchlist entries and screening run
-- (wave S1-B). DEMO ONLY - never load in production.
--   ALL NAMES BELOW ARE INVENTED. They are not, and must never be replaced by, real listed persons
--   or entities (design section 12 "Name data"; test data TD-SS-03).
--   SNSRP-203  25 entries on the three sources seeded by V1052 (AML_ADVISORY sanctions, NLDS_PEP,
--              INTERNAL): 24 ACTIVE and one delisted (INACTIVE), with aliases.
--   SNSRP-202  one failed ingestion run of the AML advisory with its failed records.
--   SNSRP-301  one completed batch screening run (PERIODIC) of the demo company FVI with a potential
--              match of the demo client CL-2026-000002 (already rated HIGH with the WATCHLIST_REVIEW
--              tag by V981, so the rules change nothing and no risk-profile row is written).
--   The entries are chosen so that no other demo client (V981, V983) matches them under the demo
--   matching criteria of V1950 (checked by ScreeningMatchingTest). The blocking keys of the entries
--   (scr_name_key) are built by the engine before its first run.
-- =====================================================================================

insert into scr_watchlist_entry (source_id, external_ref, list_type, entity_type, primary_name,
    first_name, last_name, birth_date, nationality, id_numbers, listed_on, delisted_on, status,
    effective_from, entry_version, remarks, created_at, created_by)
select s.id, e.ref, s.list_type, e.etype, e.name, null, null, e.birth::date, e.nat, e.ids,
       e.listed::date, e.delisted::date, e.status, e.listed::date, 1,
       'Demo entry - invented name', now(), 'compoff'
from (values
    -- AML advisory (sanctions)
    ('AML_ADVISORY', 'AML-DEMO-001', 'INDIVIDUAL', 'Juan de la Cruz', '1970-02-15', 'FILIPINO', null, '2025-03-01', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-002', 'INDIVIDUAL', 'Jose Miguel Lopez Reyes', '1979-11-02', 'FILIPINO', null, '2025-06-15', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-003', 'INDIVIDUAL', 'Zoltan Brakovic Invented', '1962-06-01', null, 'XP-0000301', '2024-11-20', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-004', 'ENTITY', 'Kestrel Maritime Fictional Ltd.', null, null, null, '2024-08-05', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-005', 'INDIVIDUAL', 'Orvald Nightingale Quist', '1958-01-09', null, null, '2023-05-30', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-006', 'INDIVIDUAL', 'Tamsin Vey Larkwood', '1981-12-12', null, null, '2025-01-10', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-007', 'ENTITY', 'Blackfen Trading Fictitious Corp.', null, null, null, '2024-02-14', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-008', 'INDIVIDUAL', 'Ignatius Ferro Mandalay', '1966-04-04', null, null, '2025-07-07', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-009', 'INDIVIDUAL', 'Casimir Delacroix Imaginario', '1975-10-31', null, null, '2025-09-01', null, 'ACTIVE'),
    ('AML_ADVISORY', 'AML-DEMO-010', 'ENTITY', 'Northwind Phantom Holdings', null, null, null, '2023-12-01', null, 'ACTIVE'),
    -- NLDS politically exposed persons
    ('NLDS_PEP', 'PEP-DEMO-001', 'INDIVIDUAL', 'Marietta Sandoval Reyna', '1968-08-18', 'FILIPINO', null, '2025-02-01', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-002', 'INDIVIDUAL', 'Honorato Buenaventura Lacsamana', '1955-03-25', 'FILIPINO', null, '2024-06-30', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-003', 'INDIVIDUAL', 'Leonora Quimbo Invencion', '1972-09-09', 'FILIPINO', null, '2025-04-15', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-004', 'INDIVIDUAL', 'Faustino Dimaculangan Ficticio', '1960-11-11', 'FILIPINO', null, '2024-10-01', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-005', 'INDIVIDUAL', 'Rosalinda Macaraeg Imbento', '1977-05-05', 'FILIPINO', null, '2025-05-20', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-006', 'INDIVIDUAL', 'Teodoro Villafuerte Kathangisip', '1949-07-04', 'FILIPINO', null, '2023-09-09', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-007', 'INDIVIDUAL', 'Esperanza Magbanua Likha', '1983-02-28', 'FILIPINO', null, '2025-08-08', null, 'ACTIVE'),
    ('NLDS_PEP', 'PEP-DEMO-008', 'INDIVIDUAL', 'Crisanto Padilla Gawain', '1964-12-01', 'FILIPINO', null, '2024-12-12', null, 'ACTIVE'),
    -- Internal watchlist
    ('INTERNAL', 'INT-DEMO-001', 'INDIVIDUAL', 'Gideon Farrow Pellucid', '1987-03-03', null, null, '2025-03-03', null, 'ACTIVE'),
    ('INTERNAL', 'INT-DEMO-002', 'INDIVIDUAL', 'Ysolde Marchetti Vane', '1990-10-10', null, null, '2025-06-06', null, 'ACTIVE'),
    ('INTERNAL', 'INT-DEMO-003', 'ENTITY', 'Hollowmere Assurance Brokers Fictional Inc.', null, null, null, '2024-04-04', null, 'ACTIVE'),
    ('INTERNAL', 'INT-DEMO-004', 'INDIVIDUAL', 'Percival Thorne Umbry', '1971-01-21', null, null, '2025-01-21', null, 'ACTIVE'),
    ('INTERNAL', 'INT-DEMO-005', 'INDIVIDUAL', 'Ondine Castellar Wray', '1985-06-16', null, null, '2025-02-16', null, 'ACTIVE'),
    ('INTERNAL', 'INT-DEMO-006', 'INDIVIDUAL', 'Rufus Dellamorte Fict', '1959-09-19', null, null, '2023-01-19', '2025-01-19', 'INACTIVE'),
    ('INTERNAL', 'INT-DEMO-007', 'INDIVIDUAL', 'Aurelio Montclair Nox', '1978-08-28', null, null, '2025-08-28', null, 'ACTIVE')
) as e(source, ref, etype, name, birth, nat, ids, listed, delisted, status)
join scr_watchlist_source s on s.code = e.source
on conflict (source_id, external_ref) do nothing;

insert into scr_watchlist_alias (entry_id, alias_name, alias_type, created_at, created_by)
select e.id, a.alias, 'AKA', now(), 'compoff'
from (values ('AML-DEMO-001', 'Juanito Dela Cruz'),
             ('AML-DEMO-003', 'Zoli Brakovic'),
             ('AML-DEMO-004', 'Kestrel Shipping Fictional'),
             ('PEP-DEMO-002', 'Honorato B. Lacsamana'),
             ('INT-DEMO-002', 'Ysolde Vane')) as a(ref, alias)
join scr_watchlist_entry e on e.external_ref = a.ref
where not exists (select 1 from scr_watchlist_alias x where x.entry_id = e.id and x.alias_name = a.alias);

-- ---------- One failed run of the AML advisory with its failed records (SNSRP-202) ----------
insert into scr_ingestion_run (run_no, source_id, trigger_code, file_name, received, added, updated,
    delisted, unchanged, failed, pending_approval, status, error, started_at, ended_at, created_at,
    created_by)
select 'WLR-2026-900001', s.id, 'SCHEDULED', 'aml_advisory.csv', 3, 0, 0, 0, 0, 3, false, 'FAILED',
       'No valid record in the file', timestamptz '2026-09-01 01:00:00+08',
       timestamptz '2026-09-01 01:00:05+08', now(), 'SYSTEM'
from scr_watchlist_source s
where s.code = 'AML_ADVISORY'
on conflict (run_no) do nothing;

insert into scr_ingestion_error (run_id, line_no, raw_record, reason, created_at, created_by)
select r.id, x.line_no, x.raw, x.reason, now(), 'SYSTEM'
from scr_ingestion_run r
cross join (values
    (2, 'AML-DEMO-091,INDIVIDUAL,,,,,,,,,,', 'Primary Name is required'),
    (3, 'AML-DEMO-092,INDIVIDUAL,Invented Person Two,,,,31-02-1970,,,,,', 'Birth Date 31-02-1970 is not a valid date'),
    (4, 'AML-DEMO-093,COMPANY,Invented Company Three,,,,,,,,,', 'Entity Type COMPANY is not INDIVIDUAL or ENTITY')
) as x(line_no, raw, reason)
where r.run_no = 'WLR-2026-900001'
  and not exists (select 1 from scr_ingestion_error e where e.run_id = r.id);

-- ---------- One completed batch screening run of FVI with a potential match (SNSRP-301) ----------
insert into scr_screening_run (run_no, company_id, trigger_code, reference, scope, full_rescreen,
    match_version_id, risk_version_id, clients_screened, entries_screened, matches, risk_changes,
    cases_opened, status, started_at, ended_at, created_at, created_by)
select 'SCN-2026-900001', c.id, 'PERIODIC', '2026-09-01', 'PROSPECT,CONFIRMED', true, m.id, r.id,
       (select count(*) from crm_client k where k.company_id = c.id and k.status in ('PROSPECT', 'CONFIRMED')),
       24, 1, 0, 0, 'SUCCESS', timestamptz '2026-09-01 01:30:00+08',
       timestamptz '2026-09-01 01:30:40+08', now(), 'SYSTEM'
from org_company c
join scr_config_version m on m.company_id = c.id and m.config_type = 'MATCH_CRITERIA' and m.version_no = 1
join scr_config_version r on r.company_id = c.id and r.config_type = 'RISK_RULES' and r.version_no = 1
where c.code = 'FVI'
on conflict (run_no) do nothing;

insert into scr_match (company_id, run_id, client_id, client_code, client_name, entry_id, entry_version,
    entry_name, source_code, list_type, subject_type, match_version_id, match_rule_id, score, algorithm,
    matched_fields, case_threshold, status, created_at, created_by)
select run.company_id, run.id, k.id, k.client_code, k.display_name, e.id, e.entry_version,
       e.primary_name, 'AML_ADVISORY', e.list_type, e.entity_type, run.match_version_id, mr.id, 1.0000,
       'EXACT', 'BIRTH_DATE,NAME', true, 'POTENTIAL', timestamptz '2026-09-01 01:30:20+08', 'SYSTEM'
from scr_screening_run run
join crm_client k on k.company_id = run.company_id and k.client_code = 'CL-2026-000002'
join scr_watchlist_entry e on e.external_ref = 'AML-DEMO-002'
join scr_match_rule mr on mr.version_id = run.match_version_id and mr.list_type = 'SANCTION'
    and mr.subject_type = 'INDIVIDUAL' and mr.algorithm = 'EXACT'
where run.run_no = 'SCN-2026-900001'
on conflict (client_id, entry_id, entry_version, match_version_id) do nothing;
