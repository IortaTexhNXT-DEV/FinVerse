package com.iortatechnxt.finverse.receivables;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.ReceiptRepository;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.finverse.receivables.domain.ReconciliationStatus;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankStatementService;
import com.iortatechnxt.finverse.receivables.service.DepositService;
import com.iortatechnxt.finverse.receivables.service.PdcService;
import com.iortatechnxt.finverse.support.TestData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Loads the demo profile in its own database and checks the receivables demo data set. */
@SpringBootTest
@ActiveProfiles({"test", "demo"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class ReceivablesDemoDataIT {

  @Autowired private ReceiptRepository receipts;
  @Autowired private BankReconciliationService reconciliation;
  @Autowired private BankStatementService statements;
  @Autowired private DepositService deposits;
  @Autowired private PdcService pdcs;
  @Autowired private TestData data;

  @Test
  void demoDataCoversCollectionsPdcsDepositsAndReconciliation() {
    Long company = data.company().getId();
    var all = receipts.findAll();
    assertThat(all).hasSizeGreaterThanOrEqualTo(70);
    assertThat(all)
        .allSatisfy(
            r -> {
              assertThat(r.getReceiptDate()).isAfterOrEqualTo(LocalDate.of(2026, 1, 1));
              assertThat(r.getReceiptDate()).isBeforeOrEqualTo(LocalDate.of(2026, 9, 30));
            });
    assertThat(all).anyMatch(r -> r.getStatus() == ReceiptStatus.BOUNCED);
    assertThat(all).anyMatch(r -> r.getStatus() == ReceiptStatus.PENDING_APPROVAL);
    assertThat(all).anyMatch(r -> r.unapplied().signum() > 0);
    assertThat(deposits.list(company)).isNotEmpty();
    assertThat(deposits.undeposited(company, null)).isNotEmpty();
    assertThat(pdcs.list(company, null))
        .extracting(p -> p.getStatus())
        .contains(PdcStatus.ON_HAND, PdcStatus.CLEARED, PdcStatus.RETURNED);
    assertThat(statements.list(company, "1111")).hasSize(2);

    List<BankReconciliation> recs = reconciliation.list(company);
    assertThat(recs)
        .anySatisfy(
            r -> {
              assertThat(r.getAsOfDate()).isEqualTo(LocalDate.of(2026, 8, 31));
              assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.FINALIZED);
            });
    var september = reconciliation.statement(company, "1111", LocalDate.of(2026, 9, 19));
    assertThat(september.figures().difference()).isZero();
    assertThat(september.bankDebits()).isNotEmpty();
    assertThat(september.bookCredits()).isNotEmpty();
    assertThat(september.bookDebits()).isNotEmpty();
  }
}
