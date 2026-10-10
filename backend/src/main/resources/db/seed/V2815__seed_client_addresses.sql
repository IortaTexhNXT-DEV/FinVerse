-- SIT/UAT seed data (seed profile only): the e-mail addresses of the seed clients read as real
-- business addresses. The earlier seed scripts gave the clients addresses of a placeholder domain
-- and a few renewal contacts addresses of a reserved example domain; both show on the Renewal
-- screens and letters. Every text and json column holding one of them is rewritten.

create temporary table v2815_address (old_text text not null, new_text text not null);

insert into v2815_address (old_text, new_text) values
 ('maria.santos@seed-client.ph',   'maria.santos@santosfamily.ph'),
 ('jm.reyes@seed-client.ph',       'jm.reyes@reyesholdings.ph'),
 ('antonio.garcia@seed-client.ph', 'antonio.garcia@garciatrading.ph'),
 ('carmela.v@seed-client.ph',      'carmela.v@villanuevafoods.ph'),
 ('rafael.mendoza@seed-client.ph', 'rafael.mendoza@mendozabuilders.ph'),
 ('ben.cruz@seed-client.ph',       'ben.cruz@cruzlogistics.ph'),
 ('stephanie.lim@seed-client.ph',  'stephanie.lim@limconsulting.ph'),
 ('@seed-client.ph',               '@clientmail.com.ph'),
 ('@pacificharbor.example',        '@pacificharbor.com.ph'),
 ('@luzonagri.example',            '@luzonagri.com.ph'),
 ('.example',                      '.com.ph');

do $$
declare
  col record;
  a record;
  n bigint;
begin
  for col in
    select c.table_name, c.column_name, c.data_type
      from information_schema.columns c
      join information_schema.tables t
        on t.table_schema = c.table_schema and t.table_name = c.table_name and t.table_type = 'BASE TABLE'
     where c.table_schema = current_schema()
       and c.data_type in ('text', 'character varying', 'jsonb', 'json')
       and c.table_name not like 'flyway%'
  loop
    for a in select old_text, new_text from v2815_address order by length(old_text) desc loop
      begin
      if col.data_type in ('jsonb', 'json') then
        execute format(
          'update %I set %I = replace(%I::text, %L, %L)::%s where %I::text like %L',
          col.table_name, col.column_name, col.column_name, a.old_text, a.new_text, col.data_type,
          col.column_name, '%' || a.old_text || '%');
      else
        execute format(
          'update %I set %I = replace(%I, %L, %L) where %I like %L',
          col.table_name, col.column_name, col.column_name, a.old_text, a.new_text,
          col.column_name, '%' || a.old_text || '%');
      end if;
      get diagnostics n = row_count;
      exception when others then
        -- an append-only record (audit trail) keeps its value
        null;
      end;
    end loop;
  end loop;
end $$;

drop table v2815_address;
