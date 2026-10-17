package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.collections.seed.CollectionsSeedData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the seed profile in its own database and checks the Collections storyline: the worklist
 * refreshed from the seed bookings, dispositions of every kind handed to Cashiering and Commission
 * through the outbox, a temporary reassignment and the files published with their availability,
 * each done by the SIT/UAT user whose job it is; a second start changes nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class CollectionsSeedDataIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private CollectionsSeedData seed;

  private long count(String sql) {
    Long n = jdbc.queryForObject(sql, Long.class);
    return n == null ? 0 : n;
  }

  private List<String> values(String sql) {
    return jdbc.queryForList(sql, String.class);
  }

  @Test
  void theWorklistIsRefreshedFromTheSeedBookingsAndWorked() {
    assertThat(count("select count(*) from clx_item where status = 'OPEN'")).isPositive();
    assertThat(values("select distinct current_handler from clx_item where status = 'OPEN'"))
        .isNotEmpty()
        .doesNotContainNull();
    assertThat(values("select distinct disposition_code from clx_disposition"))
        .contains(
            "COORDINATE_FURTHER", "DP_PR_FOR_REVERSAL", "PR2307_FOR_REVERSAL", "FOR_CHECK_PICKUP");
    assertThat(values("select distinct created_by from clx_disposition"))
        .containsOnly("clxhandler");
    assertThat(values("select distinct feed_code from clx_outbox where status = 'PENDING'"))
        .isNotEmpty();
    assertThat(values("select kind from clx_assignment")).contains("RULE");
    assertThat(values("select distinct frequency from clx_scheduled_file")).contains("DAILY");
    assertThat(values("select distinct created_by from clx_scheduled_file")).containsOnly("clxtl");
    assertThat(values("select head_username from cat_sales_unit where code = 'T-CBG1'"))
        .containsExactly("mkttl");
  }

  @Test
  void aSecondStartChangesNothing() {
    long dispositions = count("select count(*) from clx_disposition");
    long files = count("select count(*) from clx_scheduled_file");
    seed.run(new DefaultApplicationArguments());
    assertThat(count("select count(*) from clx_disposition")).isEqualTo(dispositions);
    assertThat(count("select count(*) from clx_scheduled_file")).isEqualTo(files);
  }
}
