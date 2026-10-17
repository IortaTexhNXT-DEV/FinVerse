package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.seed.CollectorRequestSeedData;
import com.iortatechnxt.brokerverse.cashiering.seed.UnappliedSeedPayments;
import com.iortatechnxt.brokerverse.collections.seed.UnappliedSeedData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The unapplied-payments seed storyline (wave C1-C) on a fresh seed database: the Unapplied Payment
 * Handler exists, the collectors disposed of three of the four seed payments, Cashiering applied
 * the application request, the refund request waits in Cashiering, the day's file lists the
 * application, and running the runners again changes nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class CollectionsUnappliedSeedDataIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private UnappliedSeedPayments payments;
  @Autowired private UnappliedSeedData collectors;
  @Autowired private CollectorRequestSeedData cashiers;

  private long count(String sql) {
    Long n = jdbc.queryForObject(sql, Long.class);
    return n == null ? 0 : n;
  }

  private String status(String action) {
    return jdbc.queryForObject(
        "select status from clx_application_request where action = ? order by id limit 1",
        String.class,
        action);
  }

  @Test
  void theCollectorsDisposedOfTheSeedPaymentsAndCashieringAppliedOne() {
    assertThat(
            count(
                "select count(*) from sec_user u join sec_user_role ur on ur.user_id = u.id"
                    + " join sec_role r on r.id = ur.role_id"
                    + " where u.username = 'upphandler' and r.code = 'UNAPPLIED_HANDLER'"))
        .isEqualTo(1);
    assertThat(count("select count(*) from clx_unapplied_disposition")).isEqualTo(3);
    assertThat(status("APPLY_TO_INVOICE")).isEqualTo("APPLIED");
    assertThat(status("REFUND")).isEqualTo("SENT");
    assertThat(
            count(
                "select count(*) from clx_application_request"
                    + " where action = 'APPLY_TO_INVOICE' and file_run_no is not null"))
        .isEqualTo(1);
    assertThat(count("select count(*) from csh_collector_request where status = 'QUEUED'"))
        .isEqualTo(1);
    assertThat(
            count(
                "select count(*) from csh_unapplied u where u.balance > 0 and u.stage = 'UNAPPLIED'"
                    + " and u.payor_name = 'Liza Manalo'"))
        .isEqualTo(1);
  }

  @Test
  void theRunnersChangeNothingWhenTheApplicationStartsAgain() {
    long dispositions = count("select count(*) from clx_unapplied_disposition");
    long requests = count("select count(*) from csh_collector_request");
    payments.run(new DefaultApplicationArguments());
    collectors.run(new DefaultApplicationArguments());
    cashiers.run(new DefaultApplicationArguments());
    assertThat(count("select count(*) from clx_unapplied_disposition")).isEqualTo(dispositions);
    assertThat(count("select count(*) from csh_collector_request")).isEqualTo(requests);
  }
}
