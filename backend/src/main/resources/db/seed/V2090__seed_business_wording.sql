-- iNXT BrokerVerse - V2090 Seed data in business wording (seed profile only).
-- Names, remarks and descriptions of the SIT/UAT data carried a "(seed)" marker or started with "Seed" ("Sales
-- campaign (seed)", "National Reinsurance Corp. (Seed)", "Seed case in REVIEW", "Seed actuarial basis 2026"). The
-- screens show these values, so they now read as business data: the marker is dropped and the remarks say what
-- happened. The seed scripts of the data are not changed (applied migrations); the start-up seed classes write the
-- new wording, and this script corrects the rows they wrote before. Only a value still in its seeded form is
-- changed; a value a user has since changed is kept. Rows of insert-only audit trails that cannot be changed keep
-- their wording (a notice names them).

create temporary table seed_wording (old_text text primary key, new_text text not null);

insert into seed_wording (old_text, new_text) values
 ('Seed actuarial basis 2026', 'Actuarial basis 2026'),
 ('Checked for Disbursement seed', 'Checked for disbursement'),
 ('Seed collection', 'Collection'),
 ('Seed certificate on commission and incentives', 'Certificate on commission and incentives'),
 ('Seed intake', 'Daily intake'),
 ('Seed maps reviewed', 'Maps reviewed'),
 ('Seed rows valid', 'Rows valid'),
 ('Seed load', 'Load approved'),
 ('Seed onboarding history', 'Client onboarding'),
 ('Seed entry - invented name', 'Invented name for SIT and UAT'),
 ('Seed criterion: CBG motor and property packages sold in 2026',
  'CBG motor and property packages sold in 2026'),
 ('Seed criterion: CBG motor and property packages sold in 2026 (definition pending, PQ04)',
  'CBG motor and property packages sold in 2026'),
 ('Initial seed configuration (values to be confirmed by Compliance)',
  'Initial configuration (values to be confirmed by Compliance)'),
 ('Seed-only role: insurer suite access of the SIT/UAT users until the insurer modules are removed',
  'Insurer suite access of the SIT/UAT users until the insurer modules are removed'),
 ('SIT/UAT: insurer seed stories and their tests run until the insurer modules are removed',
  'SIT/UAT: insurer stories and their tests run until the insurer modules are removed'),
 ('Seed calendar opened up to the month of the deployment', 'Calendar opened up to the month of the deployment'),
 ('uam-bulk-seed.xlsx', 'uam-bulk-requests.xlsx');

-- Workflow history: the first entry of a seeded case is the start of the case, as for a case opened on a screen.
update wf_case_history h
set action = 'start',
    comment = 'Opened in ' || s.name
from wf_case w
join wf_stage s on s.workflow_code = w.workflow_code
where w.id = h.case_id
  and s.stage_code = h.to_stage
  and h.action = 'seed'
  and (h.comment like 'Seed cycle in %' or h.comment like 'Seed case in %');

-- Submitted policies: the intake reference names the source and the assured has an address.
update sbm_policy_history
set reference = left('Received from ' || substr(reference, 6), 40)
where reference like 'Seed %';

update sbm_policy_history
set new_value = 'Ayala Avenue, Makati City'
where new_value like 'Seed address of %';

update sbm_policy_history
set old_value = 'Ayala Avenue, Makati City'
where old_value like 'Seed address of %';

update sbm_policy
set mailing_address = 'Ayala Avenue, Makati City'
where mailing_address like 'Seed address of %';

-- Every other text: the seeded values above, and names and remarks ending in a "(seed)" marker
-- ("(seed)", "(Seed)", "(SEED)", "(Seed issuer)", "(seed proposal)", "(seed data)").
-- The screening case timeline is insert-only; its seeded "Opened by ... (seed)" entries are corrected with the
-- guard off for this script only.
alter table scr_case_event disable trigger trg_scr_case_event_insert_only;

do $$
declare
  c record;
  marker constant text := '^(.*\S)\s+\((?:[Ss]eed|SEED)(?: [a-z]+)?\)$';
begin
  for c in
    select col.table_name, col.column_name
    from information_schema.columns col
    join information_schema.tables tab
      on tab.table_schema = col.table_schema
     and tab.table_name = col.table_name
     and tab.table_type = 'BASE TABLE'
    where col.table_schema = current_schema()
      and col.data_type in ('text', 'character varying')
      and col.table_name not in ('flyway_schema_history', 'seed_wording')
    order by col.table_name, col.column_name
  loop
    begin
      execute format(
          'update %I t set %I = coalesce((select w.new_text from seed_wording w where w.old_text = t.%I),'
          || ' regexp_replace(t.%I, %L, %L))'
          || ' where t.%I in (select old_text from seed_wording) or t.%I ~ %L',
          c.table_name, c.column_name, c.column_name, c.column_name, marker, '\1',
          c.column_name, c.column_name, marker);
    exception
      when others then
        raise notice 'Seed wording of %.% kept: %', c.table_name, c.column_name, sqlerrm;
    end;
  end loop;
end
$$;

alter table scr_case_event enable trigger trg_scr_case_event_insert_only;

drop table seed_wording;
