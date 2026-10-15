package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Every foreign key of the schema the migrations leave has an index whose leading columns are the
 * key's columns (in any order; partial indexes do not count), so deleting or re-keying a parent row
 * and joining from the parent do not scan the child table. {@code tools/db/fk_index_check.py}
 * writes the migration for the keys this test reports.
 */
@IntegrationTest
class ForeignKeyIndexesIT {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void everyForeignKeyHasAnIndex() {
    List<String> missing =
        jdbc.queryForList(
            """
            select t.relname || '(' || array_to_string(array(
                     select a.attname from unnest(c.conkey) with ordinality k(num, pos)
                     join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.num
                     order by k.pos), ',') || ') ' || c.conname
            from pg_constraint c
            join pg_class t on t.oid = c.conrelid
            join pg_namespace n on n.oid = t.relnamespace
            where c.contype = 'f' and n.nspname = current_schema()
              and not exists (
                select 1 from pg_index i
                where i.indrelid = c.conrelid and i.indpred is null
                  and (string_to_array(i.indkey::text, ' ')::int2[])[1:cardinality(c.conkey)]
                      @> c.conkey
                  and (string_to_array(i.indkey::text, ' ')::int2[])[1:cardinality(c.conkey)]
                      <@ c.conkey)
            order by 1
            """,
            String.class);
    assertThat(missing).as("foreign keys without an index").isEmpty();
  }
}
