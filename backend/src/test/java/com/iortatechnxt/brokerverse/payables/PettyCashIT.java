package com.iortatechnxt.brokerverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursementValues;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFund;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashStatus;
import com.iortatechnxt.brokerverse.payables.service.FundCommand;
import com.iortatechnxt.brokerverse.payables.service.PettyCashFundService;
import com.iortatechnxt.brokerverse.payables.service.PettyCashService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PettyCashIT {

  private static final LocalDate DAY = LocalDate.of(2026, 9, 3);

  @Autowired private PettyCashFundService funds;
  @Autowired private PettyCashService pettyCash;
  @Autowired private PayablesFixtures fx;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private PettyCashFund establishedFund(String imprest) {
    FundCommand cmd =
        new FundCommand(
            fx.companyId(),
            data.branch("CEB").getId(),
            PayablesFixtures.unique("PCF"),
            "Test box",
            "Test custodian",
            "1102",
            fx.bankId("BDO-CA"),
            new BigDecimal(imprest));
    PettyCashFund fund = as.run("accountant", () -> funds.create(cmd));
    assertThatThrownBy(() -> as.run("checker", () -> funds.establish(fund.getId(), DAY)))
        .isInstanceOf(BusinessRuleException.class);
    as.run("checker", () -> funds.authorize(fund.getId()));
    return as.run("checker", () -> funds.establish(fund.getId(), DAY));
  }

  private PettyCashDisbursementValues voucher(String amount) {
    return new PettyCashDisbursementValues(
        DAY, "Messenger", "5606", "FIN", "Taxi fare", "OR-1", new BigDecimal(amount));
  }

  @Test
  void imprestRuleLimitsDisbursementsAndReimbursementRestoresTheBox() {
    PettyCashFund fund = establishedFund("1000.00");
    assertThat(fund.getCashBalance()).isEqualByComparingTo("1000.00");

    assertThatThrownBy(
            () -> as.run("accountant", () -> pettyCash.disburse(fund.getId(), voucher("1000.01"))))
        .isInstanceOf(BusinessRuleException.class);

    PettyCashDisbursement first =
        as.run("accountant", () -> pettyCash.disburse(fund.getId(), voucher("700.00")));
    PettyCashDisbursement second =
        as.run("accountant", () -> pettyCash.disburse(fund.getId(), voucher("600.00")));
    assertThatThrownBy(
            () -> as.run("accountant", () -> pettyCash.approveDisbursement(first.getId())))
        .isInstanceOf(BusinessRuleException.class);
    PettyCashDisbursement approved =
        as.run("checker", () -> pettyCash.approveDisbursement(first.getId()));
    assertThat(fx.posted(approved.getJournalBatchNo(), "5606")).isEqualByComparingTo("700.00");
    assertThat(fx.posted(approved.getJournalBatchNo(), "1102")).isEqualByComparingTo("-700.00");
    assertThat(funds.get(fund.getId()).getCashBalance()).isEqualByComparingTo("300.00");

    assertThatThrownBy(() -> as.run("checker", () -> pettyCash.approveDisbursement(second.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("exceeds");
    assertThat(
            as.run("checker", () -> pettyCash.rejectDisbursement(second.getId(), "Over limit"))
                .getStatus())
        .isEqualTo(PettyCashStatus.REJECTED);

    PettyCashReimbursement claim =
        as.run(
            "accountant",
            () -> pettyCash.claimReimbursement(fund.getId(), DAY.plusDays(1), List.of(), "Refill"));
    assertThat(claim.getAmount()).isEqualByComparingTo("700.00");
    assertThat(pettyCash.claimVouchers(claim.getId())).hasSize(1);
    PettyCashReimbursement paid =
        as.run("checker", () -> pettyCash.approveReimbursement(claim.getId()));
    assertThat(fx.posted(paid.getJournalBatchNo(), "1102")).isEqualByComparingTo("700.00");
    assertThat(funds.get(fund.getId()).getCashBalance()).isEqualByComparingTo("1000.00");
    assertThat(pettyCash.unclaimed(fund.getId())).isEmpty();
    assertThat(pettyCash.reimbursements(fund.getId())).hasSize(1);
    assertThat(pettyCash.disbursements(fund.getId(), DAY, DAY)).hasSize(2);
  }

  @Test
  void rejectedClaimReleasesItsVouchers() {
    PettyCashFund fund = establishedFund("500.00");
    PettyCashDisbursement v =
        as.run("accountant", () -> pettyCash.disburse(fund.getId(), voucher("120.00")));
    as.run("checker", () -> pettyCash.approveDisbursement(v.getId()));
    PettyCashReimbursement claim =
        as.run(
            "accountant",
            () -> pettyCash.claimReimbursement(fund.getId(), DAY, List.of(v.getId()), null));
    as.run("checker", () -> pettyCash.rejectReimbursement(claim.getId(), "Missing receipts"));
    assertThat(pettyCash.unclaimed(fund.getId()))
        .extracting(PettyCashDisbursement::getId)
        .containsExactly(v.getId());
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () -> pettyCash.claimReimbursement(fund.getId(), DAY, List.of(-1L), null)))
        .isInstanceOf(BusinessRuleException.class);
  }
}
