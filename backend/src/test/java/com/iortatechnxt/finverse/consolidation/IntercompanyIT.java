package com.iortatechnxt.finverse.consolidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.consolidation.api.dto.IntercompanyTransactionRequest;
import com.iortatechnxt.finverse.consolidation.api.dto.RelationshipRequest;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyRelationship;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransaction;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransactionType;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyReconciliationService;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyReconciliationService.ReconciliationLine;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyService;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestCompanies;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntercompanyIT {

  private static final LocalDate DATE = LocalDate.of(2026, 3, 16);
  private static final LocalDate AS_OF = LocalDate.of(2026, 6, 30);

  @Autowired private IntercompanyService intercompany;
  @Autowired private IntercompanyReconciliationService reconciliation;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private CurrencyService currencies;
  @Autowired private JournalBatchRepository batches;
  @Autowired private ReportService reports;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser asUser;

  private Long parent;
  private Long subsidiary;
  private IntercompanyRelationship relationship;

  @BeforeAll
  void setUp() {
    parent = companies.create("TICA", "PHP").getId();
    subsidiary = companies.create("TICB", "USD").getId();
    companies.openYear(parent, 2026);
    companies.openYear(subsidiary, 2026);
    relationship =
        asUser.run(
            "fmanager",
            () ->
                intercompany.createRelationship(
                    new RelationshipRequest(parent, "1607", "2510", subsidiary, "1607", "2510")));
  }

  private IntercompanyTransactionRequest request(
      IntercompanyTransactionType type,
      String amount,
      String creditorAccount,
      String debtorAccount) {
    return new IntercompanyTransactionRequest(
        type,
        parent,
        subsidiary,
        DATE,
        "USD",
        new BigDecimal(amount),
        creditorAccount,
        debtorAccount,
        "Management fee",
        "FIN",
        null);
  }

  private BigDecimal balance(Long companyId, String account) {
    return ledger.netBalance(
        companyId, accounts.getByCode(companyId, account).getId(), null, AS_OF);
  }

  @Test
  void chargePostsMirrorJournalsInBothCompanies() {
    IntercompanyTransaction t =
        asUser.run(
            "fmanager",
            () ->
                intercompany.post(
                    request(IntercompanyTransactionType.CHARGE, "1000.00", "4700", "5605")));
    assertThat(t.getIcReference()).startsWith("IC-2026-");
    var creditorJournal =
        batches.findByCompanyIdAndBatchNo(parent, t.getCreditorBatchNo()).orElseThrow();
    var debtorJournal =
        batches.findByCompanyIdAndBatchNo(subsidiary, t.getDebtorBatchNo()).orElseThrow();
    assertThat(creditorJournal.getReference()).isEqualTo(t.getIcReference());
    assertThat(debtorJournal.getReference()).isEqualTo(t.getIcReference());
    assertThat(creditorJournal.getValueDate()).isEqualTo(debtorJournal.getValueDate());

    BigDecimal spot = currencies.rateOn("PHP", "USD", RateType.SPOT, DATE);
    assertThat(balance(parent, "1607"))
        .isEqualByComparingTo(Money.convert(new BigDecimal("1000"), spot));
    assertThat(balance(subsidiary, "2510")).isEqualByComparingTo("-1000.00");
    assertThat(balance(subsidiary, "5605")).isEqualByComparingTo("1000.00");

    asUser.run(
        "fmanager",
        () ->
            intercompany.post(
                request(IntercompanyTransactionType.SETTLEMENT, "400.00", "1113", "1111")));
    List<ReconciliationLine> lines = reconciliation.reconcile(parent, AS_OF);
    ReconciliationLine forward =
        lines.stream().filter(l -> l.creditorCompanyId().equals(parent)).findFirst().orElseThrow();
    assertThat(forward.dueFromFc()).isEqualByComparingTo("600.00");
    assertThat(forward.dueToFc()).isEqualByComparingTo("600.00");
    assertThat(forward.matched()).isTrue();
    assertThat(intercompany.transactions(subsidiary)).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void failureInOneCompanyPostsNothing() {
    BigDecimal before = balance(parent, "1607");
    assertThatThrownBy(
            () ->
                asUser.run(
                    "fmanager",
                    () ->
                        intercompany.post(
                            request(IntercompanyTransactionType.CHARGE, "50.00", "4700", "9999"))))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(balance(parent, "1607")).isEqualByComparingTo(before);
  }

  @Test
  void relationshipRulesAreEnforced() {
    assertThatThrownBy(
            () ->
                asUser.run(
                    "fmanager",
                    () ->
                        intercompany.createRelationship(
                            new RelationshipRequest(
                                subsidiary, "1607", "2510", parent, "1607", "2510"))))
        .isInstanceOf(DuplicateResourceException.class);
    assertThatThrownBy(
            () ->
                asUser.run(
                    "fmanager",
                    () ->
                        intercompany.createRelationship(
                            new RelationshipRequest(
                                parent, "4100", "2510", subsidiary, "1607", "2510"))))
        .hasMessageContaining("balance sheet");
    assertThatThrownBy(() -> intercompany.setActive(-1L, false))
        .isInstanceOf(ResourceNotFoundException.class);
    asUser.run("fmanager", () -> intercompany.setActive(relationship.getId(), false));
    assertThatThrownBy(
            () ->
                asUser.run(
                    "fmanager",
                    () ->
                        intercompany.post(
                            request(IntercompanyTransactionType.CHARGE, "10.00", "4700", "5605"))))
        .hasMessageContaining("No active inter-company relationship");
    asUser.run("fmanager", () -> intercompany.setActive(relationship.getId(), true));
    assertThat(intercompany.relationships(parent)).hasSize(1);
    assertThat(intercompany.relationships(null)).isNotEmpty();
  }

  @Test
  void reconciliationReportRunsAndExports() {
    Map<String, String> params =
        Map.of(
            "companyId",
            parent.toString(),
            "asOfDate",
            AS_OF.toString(),
            "mismatchesOnly",
            "false");
    asUser.run(
        "fmanager",
        () -> {
          assertThat(reports.run("GL-ICREC", params).notes()).isNotEmpty();
          for (ExportFormat format : ExportFormat.values()) {
            assertThat(reports.export("GL-ICREC", params, format).content()).isNotEmpty();
          }
          return null;
        });
  }
}
