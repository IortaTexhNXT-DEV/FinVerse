package com.iortatechnxt.brokerverse.disbursement;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Disbursement seed storyline after start-up (seed profile): payees authorised by another user,
 * a remittance voucher waiting for approval, a refund posted, a supplier check negotiated, the end
 * of day and a funding approved twice; the Operations storyline it runs after keeps its remittance
 * batch in review.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class DisbursementSeedIT {

  @Autowired private JdbcTemplate jdbc;

  private List<String> values(String sql) {
    return jdbc.queryForList(sql, String.class);
  }

  @Test
  void theStorylineCoversEveryDisbursementScreen() {
    assertThat(values("select stage from dsb_payee")).contains("ACTIVE");
    assertThat(values("select distinct created_by from dsb_payee")).containsOnly("disbtl");
    assertThat(values("select stage from dsb_voucher where disbursement_type = 'REMITTANCE'"))
        .contains("FOR_APPROVAL");
    assertThat(values("select stage from dsb_voucher where disbursement_type = 'REFUND'"))
        .contains("APPROVED");
    assertThat(values("select stage from dsb_voucher where disbursement_type = 'SUPPLIER'"))
        .contains("APPROVED", "IN_PROCESS");
    assertThat(values("select posting_status from dsb_voucher where stage = 'APPROVED'"))
        .containsOnly("POSTED");
    assertThat(values("select status from dsb_instrument")).contains("NEGOTIATED");
    assertThat(values("select status from dsb_eod_run")).isNotEmpty();
    assertThat(values("select stage from dsb_funding_request")).contains("APPROVED");
  }

  @Test
  void theApproverIsNeverTheProcessorAndOperationsKeepsItsBatchInReview() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dsb_voucher where approved_by = created_by", Long.class))
        .isZero();
    assertThat(values("select stage from rem_batch")).contains("REVIEW_IN_PROCESS");
  }
}
