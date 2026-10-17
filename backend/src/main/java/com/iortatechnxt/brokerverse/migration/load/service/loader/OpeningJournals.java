package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoiceIntake;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The opening journals of the Data Migration (DATA_MIGRATION_DESIGN 14.2 and 17.7): the provisional
 * opening trial balance (G01) and the opening-balance adjustment journals of the FY2027 true-ups
 * (G03), posted per branch and currency as system journals of type OPENING and source MIGRATION on
 * the opening value date. Lines are netted per BIBS account and cost centre; with year-end option A
 * (and C) the income and expense accounts go to the company's retained earnings account, so the new
 * fiscal year starts with a zero P&amp;L; with option B (go-live after a quarter close) the
 * year-to-date P&amp;L is opened by account.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OpeningJournals {

  private static final int RATE_SCALE = 8;

  private final SystemJournalService journals;
  private final JournalBatchRepository batches;
  private final GlAccountRepository accounts;
  private final OrganizationService organization;

  /**
   * Creates the journals.
   *
   * @param journals system journals
   * @param batches journal batches (read)
   * @param accounts chart of accounts
   * @param organization companies (retained earnings account, base currency)
   */
  public OpeningJournals(
      SystemJournalService journals,
      JournalBatchRepository batches,
      GlAccountRepository accounts,
      OrganizationService organization) {
    this.journals = journals;
    this.batches = batches;
    this.accounts = accounts;
    this.organization = organization;
  }

  /**
   * Posts an opening journal (idempotent on the source reference).
   *
   * @param header company, branch, currency, dates and references
   * @param lines trial balance or adjustment lines mapped to BIBS accounts
   * @param pnlToRetainedEarnings put income and expense on retained earnings
   * @return the posted batch
   */
  public JournalBatch post(Header header, List<Amounts> lines, boolean pnlToRetainedEarnings) {
    String retained = organization.getCompany(header.companyId()).getRetainedEarningsAccount();
    Map<String, Amounts> net = new LinkedHashMap<>();
    for (Amounts l : lines) {
      String account = l.accountCode();
      if (pnlToRetainedEarnings && !balanceSheet(header.companyId(), account)) {
        account = retained;
      }
      net.merge(
          account + "|" + Objects.toString(l.costCenter(), ""),
          new Amounts(account, l.costCenter(), l.netFc(), l.netBase()),
          Amounts::plus);
    }
    BigDecimal rate = rate(header, lines);
    List<JournalLineRequest> requests = new ArrayList<>();
    for (Amounts a : net.values()) {
      if (a.netFc().signum() != 0) {
        requests.add(
            new JournalLineRequest(
                a.accountCode(),
                a.netFc().signum() > 0 ? BalanceSide.DEBIT : BalanceSide.CREDIT,
                a.netFc().abs(),
                header.currency(),
                rate,
                header.branchId(),
                a.costCenter(),
                null,
                null,
                header.reference(),
                header.narration()));
      }
    }
    if (requests.isEmpty()) {
      throw new BusinessRuleException(
          "MIG_EMPTY_JOURNAL", "No amount to post for " + header.sourceReference());
    }
    return journals.post(request(header, requests));
  }

  /**
   * Reverses an opening journal of a rolled-back batch (same lines, sides swapped).
   *
   * @param companyId company
   * @param batchNo journal batch
   * @param valueDate value date of the reversal
   * @return the reversing batch
   */
  public JournalBatch reverse(Long companyId, String batchNo, LocalDate valueDate) {
    JournalBatch original =
        batches
            .findByCompanyIdAndBatchNo(companyId, batchNo)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_JOURNAL_MISSING", "Journal " + batchNo + " not found"));
    List<JournalLineRequest> lines = new ArrayList<>();
    for (JournalLine l : original.getLines()) {
      lines.add(
          new JournalLineRequest(
              l.getAccount().getCode(),
              l.getSide().opposite(),
              l.getAmount(),
              l.getCurrency(),
              l.getExchangeRate(),
              l.getBranchId(),
              l.getCostCenter(),
              l.getBusinessLine(),
              l.getPartyCode(),
              l.getReference(),
              "Reversal: " + Objects.toString(l.getNarration(), "")));
    }
    Header header =
        new Header(
            companyId,
            original.getBranchId(),
            original.getCurrency(),
            valueDate,
            original.getReference(),
            original.getSourceReference() + ":RB",
            "Migration batch rolled back - reversal of " + batchNo);
    return journals.post(request(header, lines));
  }

  private static SystemJournalRequest request(Header h, List<JournalLineRequest> lines) {
    return new SystemJournalRequest(
        h.companyId(),
        h.branchId(),
        JournalType.OPENING,
        h.valueDate(),
        h.currency(),
        h.narration(),
        h.reference(),
        LegacyInvoiceIntake.MODULE,
        h.sourceReference(),
        lines);
  }

  private boolean balanceSheet(Long companyId, String code) {
    return accounts
        .findByCompanyIdAndCode(companyId, code)
        .map(GlAccount::getAccountClass)
        .map(c -> c.isBalanceSheet())
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_ACCOUNT_UNKNOWN", "Account " + code + " is not in the chart of accounts"));
  }

  /** One rate for the journal: base over transaction amounts of the debit side (1 in the base). */
  private BigDecimal rate(Header header, List<Amounts> lines) {
    String base = organization.getCompany(header.companyId()).getBaseCurrency();
    if (base.equals(header.currency())) {
      return BigDecimal.ONE;
    }
    BigDecimal fc = BigDecimal.ZERO;
    BigDecimal php = BigDecimal.ZERO;
    for (Amounts l : lines) {
      if (l.netFc().signum() > 0) {
        fc = fc.add(l.netFc());
        php = php.add(l.netBase());
      }
    }
    if (fc.signum() == 0) {
      throw new BusinessRuleException(
          "MIG_RATE_MISSING", "No debit amount to derive the rate of " + header.currency());
    }
    return php.divide(fc, RATE_SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * The header of an opening journal.
   *
   * @param companyId company
   * @param branchId branch
   * @param currency transaction currency
   * @param valueDate opening value date
   * @param reference business reference (MIG-TB-&lt;as-of&gt;, MIG-TU-&lt;n&gt;)
   * @param sourceReference idempotency key (reference, branch and currency)
   * @param narration narration
   */
  public record Header(
      Long companyId,
      Long branchId,
      String currency,
      LocalDate valueDate,
      String reference,
      String sourceReference,
      String narration) {}

  /**
   * Net amounts of a BIBS account.
   *
   * @param accountCode BIBS account
   * @param costCenter cost centre, may be null
   * @param netFc debit minus credit in the transaction currency
   * @param netBase debit minus credit in the base currency
   */
  public record Amounts(
      String accountCode, String costCenter, BigDecimal netFc, BigDecimal netBase) {

    /**
     * Amounts of a line given as debit and credit.
     *
     * @param accountCode BIBS account
     * @param costCenter cost centre
     * @param debitFc debit in the transaction currency
     * @param creditFc credit in the transaction currency
     * @param debitBase debit in the base currency
     * @param creditBase credit in the base currency
     * @return amounts
     */
    public static Amounts of(
        String accountCode,
        String costCenter,
        BigDecimal debitFc,
        BigDecimal creditFc,
        BigDecimal debitBase,
        BigDecimal creditBase) {
      return new Amounts(
          accountCode,
          costCenter,
          Money.round(debitFc.subtract(creditFc)),
          Money.round(debitBase.subtract(creditBase)));
    }

    Amounts plus(Amounts other) {
      return new Amounts(
          accountCode, costCenter, netFc.add(other.netFc()), netBase.add(other.netBase()));
    }
  }
}
