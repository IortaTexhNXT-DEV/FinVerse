package com.iortatechnxt.brokerverse.finreport;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Posts a small, representative set of transactions once per test JVM so every finance report has
 * data: manual and allocation journals, a USD receipt, party postings from the accounting engine,
 * unposted (draft / pending) journals and a voucher series with a gap.
 */
@Component
class FinanceReportFixtures {

  static final String GAP_SERIES = "JV-TST-2026";

  private static boolean seeded;

  private final JournalEntryService journals;
  private final JournalAuthorizationService authorization;
  private final AccountingEventPublisher publisher;
  private final JdbcTemplate jdbc;
  private final AsUser as;
  private final TestData data;

  FinanceReportFixtures(
      JournalEntryService journals,
      JournalAuthorizationService authorization,
      AccountingEventPublisher publisher,
      JdbcTemplate jdbc,
      AsUser as,
      TestData data) {
    this.journals = journals;
    this.authorization = authorization;
    this.publisher = publisher;
    this.jdbc = jdbc;
    this.as = as;
    this.data = data;
  }

  synchronized void seed() {
    if (seeded) {
      return;
    }
    Authentication caller = SecurityContextHolder.getContext().getAuthentication();
    try {
      create();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(caller);
    }
    seeded = true;
  }

  private void create() {
    posted(
        JournalType.MANUAL,
        "Office rent",
        line("5603", BalanceSide.DEBIT, "15000.00", "FIN"),
        line("1111", BalanceSide.CREDIT, "15000.00", null));
    posted(
        JournalType.ADJUSTMENT,
        "Prepaid insurance amortisation",
        line("5605", BalanceSide.DEBIT, "4000.00", "FIN"),
        line("5610", BalanceSide.DEBIT, "1000.00", "IT"),
        line("1601", BalanceSide.CREDIT, "5000.00", null));
    posted(
        JournalType.MANUAL,
        "Transfer to USD account",
        new JournalLineRequest(
            "1113",
            BalanceSide.DEBIT,
            new BigDecimal("1000.00"),
            "USD",
            new BigDecimal("58.00"),
            null,
            null,
            null,
            null,
            "TT-1",
            "USD funding"),
        line("1112", BalanceSide.CREDIT, "58000.00", null));
    JournalBatch draft =
        as.run(
            "accountant",
            () ->
                journals.createDraft(
                    request(
                        JournalType.MANUAL,
                        "Unposted communication bill",
                        line("5604", BalanceSide.DEBIT, "700.00", "FIN"),
                        line("1111", BalanceSide.CREDIT, "700.00", null))));
    JournalBatch pending =
        as.run(
            "accountant",
            () ->
                journals.createDraft(
                    request(
                        JournalType.ACCRUAL,
                        "Accrued audit fee",
                        line("5605", BalanceSide.DEBIT, "2500.00", "FIN"),
                        line("2502", BalanceSide.CREDIT, "2500.00", null))));
    as.run("accountant", () -> journals.submit(pending.getId()));
    policy("C-000201", "127000.00");
    policy("C-000203", "56000.00");
    as.run(
        "accountant",
        () ->
            publisher.publish(
                new BusinessEvent(
                    "PREMIUM_RECEIPT",
                    data.company().getId(),
                    data.branch("CEB").getId(),
                    LocalDate.now(),
                    "PHP",
                    "RECEIPTS",
                    "FINREP-RCT-1",
                    "OR-1",
                    "C-000201",
                    null,
                    null,
                    "Premium collection",
                    Map.of("AMOUNT", new BigDecimal("27000.00")),
                    Map.of("BANK", "1111"))));
    gapSeries(draft);
  }

  private void policy(String party, String total) {
    BigDecimal due = new BigDecimal(total);
    BigDecimal gross = due.multiply(new BigDecimal("0.8"));
    as.run(
        "uw",
        () ->
            publisher.publish(
                new BusinessEvent(
                    "POLICY_ISSUE",
                    data.company().getId(),
                    data.branch("HO").getId(),
                    LocalDate.now(),
                    "PHP",
                    "UNDERWRITING",
                    "FINREP-POL-" + party,
                    "POL-" + party,
                    party,
                    "FIRE",
                    null,
                    "Fire policy issued",
                    Map.of("GROSS_PREMIUM", gross, "DST", due.subtract(gross), "TOTAL_DUE", due),
                    Map.of())));
  }

  private void gapSeries(JournalBatch template) {
    for (String no : List.of(GAP_SERIES + "-000001", GAP_SERIES + "-000004")) {
      jdbc.update(
          "insert into jnl_batch (company_id, branch_id, batch_no, journal_type, status,"
              + " transaction_date, value_date, currency, narration, total_debit, total_credit,"
              + " created_at, created_by) values (?, ?, ?, 'MANUAL', 'CANCELLED', ?, ?, 'PHP',"
              + " 'Cancelled test voucher', 0, 0, now(), 'accountant')",
          template.getCompanyId(),
          template.getBranchId(),
          no,
          LocalDate.now(),
          LocalDate.now());
    }
  }

  private void posted(JournalType type, String narration, JournalLineRequest... lines) {
    JournalBatch draft =
        as.run("accountant", () -> journals.createDraft(request(type, narration, lines)));
    as.run("accountant", () -> journals.submit(draft.getId()));
    as.run("checker", () -> authorization.approve(draft.getId()));
  }

  private JournalRequest request(JournalType type, String narration, JournalLineRequest... lines) {
    return new JournalRequest(
        data.company().getId(),
        data.branch("HO").getId(),
        type,
        LocalDate.now(),
        "PHP",
        narration,
        "FINREP",
        List.of(lines));
  }

  private static JournalLineRequest line(
      String account, BalanceSide side, String amount, String cc) {
    return new JournalLineRequest(
        account, side, new BigDecimal(amount), null, null, null, cc, null, null, null, null);
  }
}
