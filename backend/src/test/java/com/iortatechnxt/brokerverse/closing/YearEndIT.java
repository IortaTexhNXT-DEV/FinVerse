package com.iortatechnxt.brokerverse.closing;

import static com.iortatechnxt.brokerverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.closing.domain.YearEndClose;
import com.iortatechnxt.brokerverse.closing.service.CheckItem;
import com.iortatechnxt.brokerverse.closing.service.ClosingChecklistService;
import com.iortatechnxt.brokerverse.closing.service.YearEndService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.domain.FiscalYearStatus;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class YearEndIT {

  private static final LocalDate YEAR_END = LocalDate.of(2025, 12, 31);

  @Autowired private YearEndService yearEnd;
  @Autowired private ClosingChecklistService checklist;
  @Autowired private PeriodService periods;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser asUser;

  private BigDecimal balance(Long companyId, String account) {
    return ledger.netBalance(
        companyId, accounts.getByCode(companyId, account).getId(), null, YEAR_END);
  }

  private static boolean ready(List<CheckItem> items) {
    return items.stream().noneMatch(CheckItem::blocks);
  }

  @Test
  void closesProfitAndLossToRetainedEarningsAndOpensNextYear() {
    Long companyId = companies.create("TYE", "PHP").getId();
    FiscalYear year = companies.openYear(companyId, 2025);
    companies.post(
        companyId,
        LocalDate.of(2025, 3, 10),
        "PHP",
        List.of(
            line("1111", BalanceSide.DEBIT, "100000"),
            line("4100", BalanceSide.CREDIT, "100000", null, "FIRE")));
    companies.post(
        companyId,
        LocalDate.of(2025, 6, 10),
        "PHP",
        List.of(
            line("5601", BalanceSide.DEBIT, "30000", "FIN", null),
            line("1111", BalanceSide.CREDIT, "30000")));

    List<CheckItem> before = checklist.yearEnd(companyId, year.getId());
    assertThat(ready(before)).isFalse();
    assertThat(before).anyMatch(c -> c.code().equals("PERIODS_CLOSED") && !c.passed());
    assertThatThrownBy(() -> asUser.run("fmanager", () -> yearEnd.close(companyId, year.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("CLOSED or CLOSING");

    asUser.run(
        "fmanager",
        () -> {
          for (AccountingPeriod p : periods.listPeriods(year.getId())) {
            periods.startClosing(p.getId());
            if (p.getPeriodNo() < 12) {
              periods.close(p.getId());
            }
          }
          return null;
        });
    List<CheckItem> after = checklist.yearEnd(companyId, year.getId());
    assertThat(ready(after)).as(after.toString()).isTrue();
    assertThat(yearEnd.preview(companyId, year.getId())).hasSize(2);

    YearEndClose record = asUser.run("fmanager", () -> yearEnd.close(companyId, year.getId()));
    assertThat(record.getNetResult()).isEqualByComparingTo("70000");
    assertThat(record.getNextYearCode()).isEqualTo(2026);
    assertThat(balance(companyId, "4100")).isEqualByComparingTo("0");
    assertThat(balance(companyId, "5601")).isEqualByComparingTo("0");
    assertThat(balance(companyId, "3500")).isEqualByComparingTo("-70000");
    assertThat(balance(companyId, "1111")).isEqualByComparingTo("70000");

    assertThat(periods.getYear(year.getId()).getStatus()).isEqualTo(FiscalYearStatus.CLOSED);
    assertThat(periods.listPeriods(year.getId()))
        .allMatch(p -> p.getStatus() == PeriodStatus.CLOSED);
    FiscalYear next = periods.yearContaining(companyId, LocalDate.of(2026, 1, 1));
    assertThat(next.getYearCode()).isEqualTo(2026);
    assertThat(periods.listPeriods(next.getId()).get(0).getStatus()).isEqualTo(PeriodStatus.OPEN);
    assertThat(yearEnd.closeRecord(year.getId())).isPresent();
    assertThat(yearEnd.preview(companyId, year.getId())).isEmpty();

    assertThatThrownBy(() -> asUser.run("fmanager", () -> yearEnd.close(companyId, year.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }
}
