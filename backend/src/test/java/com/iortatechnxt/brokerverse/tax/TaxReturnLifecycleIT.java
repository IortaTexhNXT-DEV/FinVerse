package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.domain.FilingFrequency;
import com.iortatechnxt.brokerverse.tax.domain.RemittanceFacts;
import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.TaxForm;
import com.iortatechnxt.brokerverse.tax.domain.TaxRemittance;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxFormCommand;
import com.iortatechnxt.brokerverse.tax.service.TaxFormService;
import com.iortatechnxt.brokerverse.tax.service.TaxReturnService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Returns lifecycle DRAFT → FILED → PAID with the remittance posting that clears the tax payable,
 * four-eyes filing, cancellation and duplicate protection. Uses September 2026 monthly periods,
 * which the seed data never prepares.
 */
@IntegrationTest
class TaxReturnLifecycleIT {

  private static final LocalDate SEPT_1 = LocalDate.of(2026, 9, 1);
  private static final LocalDate SEPT_30 = LocalDate.of(2026, 9, 30);
  private static final String BANK = "BDO-CA";

  @Autowired private TaxReturnService returns;
  @Autowired private TaxFormService forms;
  @Autowired private TaxFixtures fixtures;
  @Autowired private AsUser as;

  @BeforeEach
  void masters() {
    fixtures.masters();
  }

  @Test
  void dstReturnIsFiledByAnotherUserAndItsPaymentClearsThePayable() {
    fixtures.firePolicy(LocalDate.of(2026, 9, 12), "80000");
    Long company = fixtures.companyId();
    TaxReturn draft = as.run("accountant", () -> returns.create(company, "2000", SEPT_1));
    assertThat(draft.getStatus()).isEqualTo(ReturnStatus.DRAFT);
    assertThat(draft.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(draft.getAmountPayable()).isPositive();
    assertThat(draft.getLines()).isNotEmpty();
    assertThatThrownBy(() -> as.run("accountant", () -> returns.create(company, "2000", SEPT_1)))
        .isInstanceOf(DuplicateResourceException.class);

    TaxReturn refreshed = as.run("accountant", () -> returns.refresh(draft.getId()));
    assertThat(refreshed.getAmountPayable()).isEqualByComparingTo(draft.getAmountPayable());
    assertThatThrownBy(
            () -> as.run("accountant", () -> returns.file(draft.getId(), SEPT_30, "EFPS-1")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("prepared");

    TaxReturn filed = as.run("checker", () -> returns.file(draft.getId(), SEPT_30, "EFPS-1"));
    assertThat(filed.getStatus()).isEqualTo(ReturnStatus.FILED);
    assertThat(filed.getFiledBy()).isEqualTo("checker");
    BigDecimal ledgerBefore = fixtures.creditMovement("2503", SEPT_1, SEPT_30);

    TaxReturn paid =
        as.run(
            "checker",
            () -> returns.pay(draft.getId(), new RemittanceFacts(SEPT_30, BANK, "PAY-1")));
    assertThat(paid.getStatus()).isEqualTo(ReturnStatus.PAID);
    TaxRemittance remittance = returns.remittance(draft.getId()).orElseThrow();
    BigDecimal amount = draft.getAmountPayable();
    assertThat(remittance.getAmount()).isEqualByComparingTo(amount);
    assertThat(remittance.getPayableCleared()).isEqualByComparingTo(amount);
    assertThat(remittance.isLate()).isFalse();
    assertThat(fixtures.posted(remittance.getJournalBatchNo(), "2503"))
        .isEqualByComparingTo(amount);
    assertThat(fixtures.posted(remittance.getJournalBatchNo(), "1111"))
        .isEqualByComparingTo(amount.negate());
    assertThat(fixtures.creditMovement("2503", SEPT_1, SEPT_30))
        .isEqualByComparingTo(ledgerBefore.subtract(amount));
    assertThat(returns.remittances(company, SEPT_1, SEPT_30))
        .extracting(TaxRemittance::getReturnId)
        .contains(draft.getId());
    assertThatThrownBy(() -> as.run("checker", () -> returns.cancel(draft.getId(), "late")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void vatStyleFormAppliesInputVatAndPaysTheBalance() {
    fixtures.firePolicy(LocalDate.of(2026, 9, 20), "50000");
    fixtures.supplierInvoice(LocalDate.of(2026, 9, 20), "20000");
    Long company = fixtures.companyId();
    TaxForm monthlyVat = testVatForm(company);
    TaxReturn r = as.run("accountant", () -> returns.create(company, monthlyVat.getCode(), SEPT_1));
    assertThat(r.getTaxCredits()).isPositive();
    as.run("checker", () -> returns.file(r.getId(), SEPT_30, "EFPS-VAT"));
    as.run("checker", () -> returns.pay(r.getId(), new RemittanceFacts(SEPT_30, BANK, "PAY-VAT")));

    TaxRemittance remittance = returns.remittance(r.getId()).orElseThrow();
    String batch = remittance.getJournalBatchNo();
    assertThat(remittance.getPayableCleared()).isEqualByComparingTo(r.getTaxDue());
    assertThat(remittance.getCreditApplied().add(remittance.getAmount()))
        .isEqualByComparingTo(r.getTaxDue());
    assertThat(fixtures.posted(batch, "2504")).isEqualByComparingTo(r.getTaxDue());
    assertThat(fixtures.posted(batch, "1603"))
        .isEqualByComparingTo(remittance.getCreditApplied().negate());
    assertThat(fixtures.posted(batch, "1111")).isEqualByComparingTo(r.getAmountPayable().negate());
  }

  @Test
  void draftsCanBeCancelledAndPreparedAgain() {
    Long company = fixtures.companyId();
    TaxReturn first = as.run("accountant", () -> returns.create(company, "FST", SEPT_1));
    TaxReturn cancelled = as.run("accountant", () -> returns.cancel(first.getId(), "recompute"));
    assertThat(cancelled.getStatus()).isEqualTo(ReturnStatus.CANCELLED);
    assertThat(cancelled.getStatusReason()).isEqualTo("recompute");
    TaxReturn second = as.run("accountant", () -> returns.create(company, "FST", SEPT_1));
    assertThat(second.getId()).isNotEqualTo(first.getId());
    assertThat(returns.list(company, 2026, "FST", ReturnStatus.CANCELLED))
        .extracting(TaxReturn::getId)
        .contains(first.getId());
    assertThatThrownBy(
            () ->
                as.run(
                    "checker",
                    () -> returns.pay(second.getId(), new RemittanceFacts(SEPT_30, BANK, "X"))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void untrackedAndMisalignedPeriodsAreRejected() {
    Long company = fixtures.companyId();
    assertThatThrownBy(() -> as.run("accountant", () -> returns.create(company, "1601-C", SEPT_1)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant", () -> returns.create(company, "2550Q", LocalDate.of(2026, 8, 1))))
        .isInstanceOf(BusinessRuleException.class);
  }

  private TaxForm testVatForm(Long company) {
    String code = "VAT-M-TEST";
    return forms.list(company).stream()
        .filter(f -> f.getCode().equals(code))
        .findFirst()
        .orElseGet(
            () -> {
              TaxForm created =
                  as.run(
                      "accountant",
                      () ->
                          forms.create(
                              new TaxFormCommand(
                                  company,
                                  code,
                                  "Monthly VAT (test form)",
                                  TaxAuthority.BIR,
                                  FilingFrequency.MONTHLY,
                                  WorksheetKind.VAT,
                                  1,
                                  25,
                                  "2504",
                                  "1603",
                                  true,
                                  LocalDate.of(2026, 9, 1))));
              return as.run("checker", () -> forms.authorize(created.getId()));
            });
  }
}
