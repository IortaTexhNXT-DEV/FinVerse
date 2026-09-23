package com.iortatechnxt.finverse.closing.demo;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.consolidation.api.dto.IntercompanyTransactionRequest;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransactionType;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.finverse.journal.service.SystemJournalService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demo journals of the subsidiary FVS (USD): share capital (80 % subscribed by FVI), monthly
 * premium, claims, commission and salaries January–September 2026, and inter-company management
 * fees charged by FVI (March–August) with one settlement.
 *
 * <p>Idempotent: journals use fixed source references; inter-company transactions are created only
 * when the subsidiary has none.
 */
class DemoSubsidiaryData {

  private static final String SOURCE = "PLANNING_DEMO";
  private static final String USD = "USD";
  private static final int YEAR = 2026;
  private static final int LAST_MONTH = 9;
  private static final int POSTING_DAY = 20;
  private static final int FEE_DAY = 15;
  private static final int FIRST_FEE_MONTH = 3;
  private static final int LAST_FEE_MONTH = 8;
  private static final String BANK = "1111";
  private static final LocalDate CAPITAL_DATE = LocalDate.of(2026, 1, 5);
  private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 6, 30);
  private static final int PREMIUM_BASE = 120_000;
  private static final int PREMIUM_GROWTH = 5_000;
  private static final int CLAIMS_BASE = 45_000;
  private static final int CLAIMS_GROWTH = 2_500;
  private static final String FIRE = "FIRE";

  private final SystemJournalService journals;
  private final IntercompanyService intercompany;
  private final OrganizationService organization;

  DemoSubsidiaryData(
      SystemJournalService journals,
      IntercompanyService intercompany,
      OrganizationService organization) {
    this.journals = journals;
    this.intercompany = intercompany;
    this.organization = organization;
  }

  /**
   * Loads the journals.
   *
   * @param parent parent company (FVI)
   * @param subsidiary subsidiary (FVS)
   */
  void load(Company parent, Company subsidiary) {
    post(
        subsidiary,
        "FVS-CAPITAL",
        JournalType.RECEIPT,
        CAPITAL_DATE,
        "Share capital subscribed",
        List.of(
            line(BANK, BalanceSide.DEBIT, "2000000", null, null),
            line("3100", BalanceSide.CREDIT, "2000000", null, null)));
    post(
        parent,
        "FVI-INVEST-FVS",
        JournalType.INVESTMENT,
        CAPITAL_DATE,
        "80% of FVS share capital acquired by share issue",
        List.of(
            line("1506", BalanceSide.DEBIT, "1600000", null, null),
            line("3200", BalanceSide.CREDIT, "1600000", null, null)));
    for (int m = 1; m <= LAST_MONTH; m++) {
      monthly(subsidiary, m);
    }
    if (intercompany.transactions(subsidiary.getId()).isEmpty()) {
      for (int m = FIRST_FEE_MONTH; m <= LAST_FEE_MONTH; m++) {
        managementFee(parent, subsidiary, LocalDate.of(YEAR, m, FEE_DAY));
      }
      intercompany.post(
          new IntercompanyTransactionRequest(
              IntercompanyTransactionType.SETTLEMENT,
              parent.getId(),
              subsidiary.getId(),
              SETTLEMENT_DATE,
              USD,
              new BigDecimal("60000.00"),
              "1113",
              BANK,
              "Partial settlement of management fees",
              null,
              null));
    }
  }

  private void monthly(Company fvs, int month) {
    LocalDate date = LocalDate.of(YEAR, month, POSTING_DAY);
    String premium = String.valueOf(PREMIUM_BASE + month * PREMIUM_GROWTH);
    String claims = String.valueOf(CLAIMS_BASE + month * CLAIMS_GROWTH);
    post(
        fvs,
        "FVS-PREMIUM-" + month,
        JournalType.PREMIUM,
        date,
        "Monthly premium written",
        List.of(
            line(BANK, BalanceSide.DEBIT, premium, null, null),
            line("4100", BalanceSide.CREDIT, premium, FIRE, null)));
    post(
        fvs,
        "FVS-CLAIMS-" + month,
        JournalType.CLAIMS,
        date,
        "Monthly claims paid",
        List.of(
            line("5100", BalanceSide.DEBIT, claims, FIRE, null),
            line(BANK, BalanceSide.CREDIT, claims, null, null)));
    post(
        fvs,
        "FVS-OPEX-" + month,
        JournalType.PAYMENT,
        date,
        "Salaries and commission",
        List.of(
            line("5601", BalanceSide.DEBIT, "30000", null, "FIN"),
            line("5400", BalanceSide.DEBIT, "15000", FIRE, null),
            line(BANK, BalanceSide.CREDIT, "45000", null, null)));
  }

  private void managementFee(Company fvi, Company fvs, LocalDate date) {
    intercompany.post(
        new IntercompanyTransactionRequest(
            IntercompanyTransactionType.CHARGE,
            fvi.getId(),
            fvs.getId(),
            date,
            USD,
            new BigDecimal("25000.00"),
            "4700",
            "5605",
            "Group management fee " + date.getMonth(),
            "FIN",
            null));
  }

  private void post(
      Company company,
      String key,
      JournalType type,
      LocalDate date,
      String narration,
      List<JournalLineRequest> lines) {
    journals.post(
        new SystemJournalRequest(
            company.getId(),
            headOffice(company),
            type,
            date,
            USD,
            narration,
            key,
            SOURCE,
            key,
            lines));
  }

  private Long headOffice(Company company) {
    return organization.listBranches(company.getId()).stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private static JournalLineRequest line(
      String account, BalanceSide side, String amount, String businessLine, String costCenter) {
    return new JournalLineRequest(
        account,
        side,
        new BigDecimal(amount),
        null,
        null,
        null,
        costCenter,
        businessLine,
        null,
        null,
        null);
  }
}
