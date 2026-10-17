-- Seed data texts without internal references (work item MSG0), the seed counterpart of
-- V1110__user_text_without_internal_references. Runs after all seed migrations. Each text is
-- replaced only where it still holds the seeded value.

create temporary table msg0_text (tbl varchar(63) not null, old_text text not null, new_text text not null);

insert into msg0_text (tbl, old_text, new_text) values
 ('bkg_incentive_rule',
  'Placeholder: CBG motor MTR10 campaign 2026 (qualification rules pending, Q33)',
  'Placeholder: CBG motor MTR10 campaign 2026'),
 ('cat_incentive_criteria',
  'Placeholder: CBG motor MTR10 campaign 2026 (qualification rules pending, Q33)',
  'Placeholder: CBG motor MTR10 campaign 2026'),
 ('cat_incentive_criteria',
  'Seed criterion: CBG motor and property packages sold in 2026 (definition pending, PQ04)',
  'Seed criterion: CBG motor and property packages sold in 2026'),
 ('cmr_incentive_scheme',
  'Fixed amount per policy by minimum basic premium, passed on to branches - amounts from BDOI (OQ39)',
  'Fixed amount per policy by minimum basic premium, passed on to branches - amounts from BDOI'),
 ('cmr_incentive_scheme',
  'Retail / Corporate production targets with rates and multipliers per period - tiers from BDOI (OQ39)',
  'Retail / Corporate production targets with rates and multipliers per period - tiers from BDOI'),
 ('lov_value',
  'Placeholder: amount not commensurate with the profile (to be replaced, SQ09)',
  'Placeholder: amount not commensurate with the profile'),
 ('lov_value',
  'Placeholder: no apparent economic purpose (to be replaced, SQ09)',
  'Placeholder: no apparent economic purpose'),
 ('lov_value',
  'Placeholder: transaction with a listed person (to be replaced, SQ09)',
  'Placeholder: transaction with a listed person'),
 ('rem_incentive_rule',
  'Placeholder: 2% early remittance incentive, CBG fire, within 30 days of inception (rates parked, OQ23)',
  'Placeholder: 2% early remittance incentive, CBG fire, within 30 days of inception'),
 ('scr_template_field',
  'AMLC reason codes to be supplied (SQ09)',
  'AMLC reason codes to be supplied');

do $$
declare
  c record;
begin
  for c in
    select distinct m.tbl, col.column_name
    from msg0_text m
    join information_schema.columns col
      on col.table_schema = current_schema()
     and col.table_name = m.tbl
     and col.data_type in ('text', 'character varying')
  loop
    execute format(
        'update %I t set %I = m.new_text from msg0_text m where m.tbl = %L and t.%I = m.old_text',
        c.tbl, c.column_name, c.tbl, c.column_name);
  end loop;
end
$$;

drop table msg0_text;
