package com.iortatechnxt.brokerverse.csf.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.csf.service.CsfStatusRules.Facts;
import com.iortatechnxt.brokerverse.csf.service.CsfStatusRules.Row;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The CSF status mapping of FRS section 5.1 (BRCSF-005 / 5.001, CSF-EM07). */
class CsfStatusRulesTest {

  private static final Facts PAID_IN_FORCE = new Facts(false, false);
  private static final Facts OUTSTANDING = new Facts(true, false);
  private static final Facts EXPIRED = new Facts(false, true);

  private static final CsfStatusRules SEED =
      CsfStatusRules.of(
          List.of(
              new Row("CANCELLED", "CLOSED"),
              new Row("VOIDED", "CLOSED"),
              new Row("BOOKED_EXPIRED", "CLOSED"),
              new Row("BOOKED_OUTSTANDING", "BOOKED"),
              new Row("BOOKED", "OPEN"),
              new Row("DRAFT", "PENDING"),
              new Row("SUBMITTED", "PENDING"),
              new Row("AWAITING_PAYMENT", "AWAITING"),
              new Row("PLACED", "AWAITING"),
              new Row("ORPHAN", null)));

  @Test
  void theSeedMappingGivesTheFiveStatuses() {
    assertThat(SEED.statusOf(AccountStatus.DRAFT, PAID_IN_FORCE)).contains("PENDING");
    assertThat(SEED.statusOf(AccountStatus.AWAITING_PAYMENT, OUTSTANDING)).contains("AWAITING");
    assertThat(SEED.statusOf(AccountStatus.BOOKED, OUTSTANDING)).contains("BOOKED");
    assertThat(SEED.statusOf(AccountStatus.BOOKED, PAID_IN_FORCE)).contains("OPEN");
    assertThat(SEED.statusOf(AccountStatus.BOOKED, EXPIRED)).contains("CLOSED");
    assertThat(SEED.statusOf(AccountStatus.CANCELLED, OUTSTANDING)).contains("CLOSED");
    assertThat(SEED.statusOf(AccountStatus.POLICY_ISSUED, PAID_IN_FORCE)).isEmpty();
    assertThat(SEED.rules()).hasSize(9);
  }

  @Test
  void theFirstMatchingRowAppliesAndAnyCoversEveryStage() {
    CsfStatusRules changed =
        CsfStatusRules.of(
            List.of(
                new Row("ANY_EXPIRED", "CLOSED"),
                new Row("ANY", "OPEN"),
                new Row("DRAFT", "PENDING")));
    assertThat(changed.statusOf(AccountStatus.DRAFT, EXPIRED)).contains("CLOSED");
    assertThat(changed.statusOf(AccountStatus.DRAFT, PAID_IN_FORCE)).contains("OPEN");
    assertThat(changed.statusOf(AccountStatus.PLACED, OUTSTANDING)).contains("OPEN");
  }
}
