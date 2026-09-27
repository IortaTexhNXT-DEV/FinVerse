package com.iortatechnxt.brokerverse.commission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** DP list naming convention and rows (CMRID.001) and insurer decisions (CMRID.009). */
class DpListHandlerTest {

  @Test
  void theFileNameGivesTheBranchAndTheDate() {
    Origin ho = DpListHandler.origin("HO_DP_20260930.xlsx");
    assertThat(ho.source()).isEqualTo(ListSource.HEAD_OFFICE);
    assertThat(ho.submissionDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    Origin branch = DpListHandler.origin("Cebu Main_DP_20260915.csv");
    assertThat(branch.source()).isEqualTo(ListSource.BRANCH);
    assertThat(branch.branchCode()).isEqualTo("CEBU MAIN");
  }

  @Test
  void aFileBreakingTheConventionIsRefused() {
    assertThatThrownBy(() -> DpListHandler.origin("dp-list.xlsx"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("<Branch>_DP_<yyyyMMdd>");
    assertThatThrownBy(() -> DpListHandler.origin("HO_DP_20261399.xlsx"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not a date");
    assertThatThrownBy(() -> DpListHandler.origin(null)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void rowsBecomeSubmissions() {
    Origin origin = DpListHandler.origin("HO_DP_20260930.csv");
    Submission s =
        DpListHandler.submission(
            Map.of(
                "Invoice No.", " BI-1 ",
                "Policy No.", "POL-1",
                "Insurer", "ins-mgic",
                "Premium", "28,506.63",
                "Remarks", "paid"),
            origin);
    assertThat(s.invoiceNo()).isEqualTo("BI-1");
    assertThat(s.insurerCode()).isEqualTo("INS-MGIC");
    assertThat(s.premium()).isEqualByComparingTo("28506.63");
    assertThat(s.branchCode()).isEqualTo("HO");
    assertThat(DpListHandler.submission(Map.of("Invoice No.", "BI-2"), origin).premium()).isNull();
    assertThatThrownBy(() -> DpListHandler.submission(Map.of("Premium", "1"), origin))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () -> DpListHandler.submission(Map.of("Invoice No.", "BI-3", "Premium", "x"), origin))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void insurerDecisionsAreRead() {
    assertThat(DpResponseHandler.approved("approved")).isTrue();
    assertThat(DpResponseHandler.approved(" Y ")).isTrue();
    assertThat(DpResponseHandler.approved("REJECTED")).isFalse();
    assertThat(DpResponseHandler.approved("n")).isFalse();
    assertThatThrownBy(() -> DpResponseHandler.approved("maybe"))
        .isInstanceOf(BusinessRuleException.class);
  }
}
