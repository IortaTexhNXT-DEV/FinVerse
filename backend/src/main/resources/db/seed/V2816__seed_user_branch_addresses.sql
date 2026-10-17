-- The e-mail addresses of the SIT/UAT users, branches, payees and other records read as addresses
-- of the UAT environment (uat.brokerverse.cloud) instead of the earlier placeholder domain, in every
-- text column that holds one. Insert-only audit and log tables keep their history as recorded.
do $$
declare
  col record;
begin
  for col in
    select c.table_schema, c.table_name, c.column_name
      from information_schema.columns c
      join information_schema.tables t
        on t.table_schema = c.table_schema and t.table_name = c.table_name
     where c.table_schema = current_schema()
       and t.table_type = 'BASE TABLE'
       and c.data_type in ('character varying', 'text')
       and c.table_name not like 'flyway%'
  loop
    begin
    execute format(
      'update %I.%I set %I = replace(%I, %L, %L) where %I like %L',
      col.table_schema, col.table_name, col.column_name, col.column_name,
      '@brokerverse-seed.ph', '@uat.brokerverse.cloud', col.column_name, '%@brokerverse-seed.ph%');
    exception when others then
      -- an insert-only table refuses updates: its rows stay as recorded
      null;
    end;
  end loop;
end $$;
