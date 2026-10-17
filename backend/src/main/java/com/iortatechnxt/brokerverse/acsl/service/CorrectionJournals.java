package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the journals a correction works on (ACSL 2.5.3, 2.9.0-2.9.1): the posted lines of the
 * journals of an invoice family (the left side of the correction editor), one original line, and
 * the accounts of the correction lines.
 */
@Component
@Transactional(readOnly = true)
public class CorrectionJournals {

  private final JournalBatchRepository journals;
  private final InvoiceLedgerQueryService ledger;
  private final ChartOfAccountsService accounts;

  /**
   * Creates the reader.
   *
   * @param journals journal batches
   * @param ledger Operations invoice ledger
   * @param accounts chart of accounts
   */
  public CorrectionJournals(
      JournalBatchRepository journals,
      InvoiceLedgerQueryService ledger,
      ChartOfAccountsService accounts) {
    this.journals = journals;
    this.ledger = ledger;
    this.accounts = accounts;
  }

  /**
   * The posted lines of the journals of an invoice family and of an extra journal.
   *
   * @param companyId company
   * @param invoiceNo invoice (its whole family is read), may be null
   * @param extraBatchNo another journal, may be null
   * @return lines, by journal and line number
   */
  public List<OriginalLine> familyLines(Long companyId, String invoiceNo, String extraBatchNo) {
    Set<String> batchNos = new LinkedHashSet<>();
    if (invoiceNo != null) {
      for (OpsInvoice member : ledger.family(invoiceNo)) {
        ledger.movements(member.getInvoiceNo()).stream()
            .map(OpsInvoiceMovement::getJournalBatchNo)
            .filter(Objects::nonNull)
            .forEach(batchNos::add);
      }
    }
    if (extraBatchNo != null) {
      batchNos.add(extraBatchNo);
    }
    List<OriginalLine> lines = new ArrayList<>();
    for (String batchNo : batchNos) {
      journals
          .findByCompanyIdAndBatchNo(companyId, batchNo)
          .ifPresent(b -> b.getLines().forEach(l -> lines.add(OriginalLine.of(b, l))));
    }
    return lines;
  }

  /**
   * One line of a posted journal.
   *
   * @param companyId company
   * @param batchNo journal
   * @param lineNo line
   * @return line
   */
  public OriginalLine line(Long companyId, String batchNo, int lineNo) {
    JournalBatch batch = batch(companyId, batchNo);
    return batch.getLines().stream()
        .filter(l -> l.getLineNo() == lineNo)
        .findFirst()
        .map(l -> OriginalLine.of(batch, l))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "ACSL_LINE_UNKNOWN", "Journal " + batchNo + " has no line " + lineNo));
  }

  /**
   * A posted journal of the company.
   *
   * @param companyId company
   * @param batchNo journal
   * @return journal
   */
  public JournalBatch batch(Long companyId, String batchNo) {
    return journals
        .findByCompanyIdAndBatchNo(companyId, batchNo)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "ACSL_JOURNAL_UNKNOWN", "Journal " + batchNo + " not found"));
  }

  /**
   * A journal of the company, if it exists.
   *
   * @param companyId company
   * @param batchNo journal
   * @return journal
   */
  public Optional<JournalBatch> findBatch(Long companyId, String batchNo) {
    return batchNo == null
        ? Optional.empty()
        : journals.findByCompanyIdAndBatchNo(companyId, batchNo);
  }

  /**
   * The journal a correction is taken from: the corrected journal, else the first journal of the
   * invoice (its booking).
   *
   * @param companyId company
   * @param batchNo corrected journal, may be null
   * @param invoiceNo invoice, may be null
   * @return journal
   */
  public Optional<JournalBatch> sourceBatch(Long companyId, String batchNo, String invoiceNo) {
    Optional<JournalBatch> given = findBatch(companyId, batchNo);
    if (given.isPresent() || invoiceNo == null) {
      return given;
    }
    return ledger.movements(invoiceNo).stream()
        .map(OpsInvoiceMovement::getJournalBatchNo)
        .filter(Objects::nonNull)
        .findFirst()
        .flatMap(no -> journals.findByCompanyIdAndBatchNo(companyId, no));
  }

  /**
   * A postable account of the chart.
   *
   * @param companyId company
   * @param code account code
   * @return account
   */
  public GlAccount account(Long companyId, String code) {
    try {
      GlAccount account = accounts.getByCode(companyId, code);
      if (!account.isPostable()) {
        throw new BusinessRuleException(
            "ACSL_ACCOUNT_NOT_POSTABLE", "Account " + code + " is a heading");
      }
      return account;
    } catch (ResourceNotFoundException e) {
      throw new BusinessRuleException(
          "ACSL_ACCOUNT_UNKNOWN", "Account " + code + " is not in the chart", e);
    }
  }

  /**
   * A posted journal line (left side of the correction editor).
   *
   * @param batchNo journal
   * @param journalType journal type
   * @param valueDate value date
   * @param lineNo line
   * @param accountCode account
   * @param accountName account name
   * @param side side
   * @param amount amount
   * @param partyCode party
   * @param costCenter cost centre
   * @param businessLine line of business
   * @param narration narration
   */
  public record OriginalLine(
      String batchNo,
      String journalType,
      LocalDate valueDate,
      int lineNo,
      String accountCode,
      String accountName,
      BalanceSide side,
      BigDecimal amount,
      String partyCode,
      String costCenter,
      String businessLine,
      String narration) {

    /**
     * Maps a journal line.
     *
     * @param b journal
     * @param l line
     * @return view
     */
    public static OriginalLine of(JournalBatch b, JournalLine l) {
      return new OriginalLine(
          b.getBatchNo(),
          b.getJournalType().name(),
          b.getValueDate(),
          l.getLineNo(),
          l.getAccount().getCode(),
          l.getAccount().getName(),
          l.getSide(),
          l.getAmount(),
          l.getPartyCode(),
          l.getCostCenter(),
          l.getBusinessLine(),
          l.getNarration());
    }
  }
}
