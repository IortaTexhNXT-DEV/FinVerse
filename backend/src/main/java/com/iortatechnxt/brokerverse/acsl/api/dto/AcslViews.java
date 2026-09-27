package com.iortatechnxt.brokerverse.acsl.api.dto;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.CaseOutcome;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseSubject;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLine;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Response records of the ACSL endpoints (ACSL 2.4-2.16). */
public interface AcslViews {

  /**
   * A case.
   *
   * @param id id
   * @param caseNo case number
   * @param type type
   * @param stage stage
   * @param account invoice, root, insurer, client, AR, currency and amount
   * @param subject subject
   * @param details details
   * @param requesterModule requesting module
   * @param requesterRef its reference
   * @param requestedBy requesting user
   * @param findings findings
   * @param outcome result
   * @param resultRemarks result remarks
   * @param resultBy who gave the result
   * @param resultAt when
   * @param correctionId correction raised
   * @param reversalRef payment reversal reference
   * @param reversalStatus payment reversal status
   * @param reversalMessage payment reversal message
   * @param createdAt opened at
   */
  record CaseView(
      Long id,
      String caseNo,
      CaseType type,
      CaseStage stage,
      CaseSubject account,
      String subject,
      String details,
      String requesterModule,
      String requesterRef,
      String requestedBy,
      String findings,
      CaseOutcome outcome,
      String resultRemarks,
      String resultBy,
      Instant resultAt,
      Long correctionId,
      String reversalRef,
      String reversalStatus,
      String reversalMessage,
      Instant createdAt) {

    /**
     * Maps a case.
     *
     * @param c case
     * @return view
     */
    public static CaseView from(AcslCase c) {
      return new CaseView(
          c.getId(),
          c.getCaseNo(),
          c.getCaseType(),
          c.getStage(),
          c.accountOrNone(),
          c.getSubject(),
          c.getDetails(),
          c.getRequesterModule(),
          c.getRequesterRef(),
          c.getRequestedBy(),
          c.getFindings(),
          c.getOutcome(),
          c.getResultRemarks(),
          c.getResultBy(),
          c.getResultAt(),
          c.getCorrectionId(),
          c.getReversalRef(),
          c.getReversalStatus(),
          c.getReversalMessage(),
          c.getCreatedAt());
    }
  }

  /**
   * A correction line.
   *
   * @param lineNo line
   * @param accountCode account
   * @param side side
   * @param amount amount
   * @param partyCode party
   * @param invoiceNo invoice
   * @param component ledger component
   * @param costCenter cost centre
   * @param businessLine line of business
   * @param narration narration
   * @param origin origin
   * @param originalBatchNo original journal
   * @param originalLineNo original line
   */
  record LineView(
      int lineNo,
      String accountCode,
      BalanceSide side,
      BigDecimal amount,
      String partyCode,
      String invoiceNo,
      String component,
      String costCenter,
      String businessLine,
      String narration,
      LineOrigin origin,
      String originalBatchNo,
      Integer originalLineNo) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static LineView from(CorrectionLine l) {
      return new LineView(
          l.getLineNo(),
          l.getAccountCode(),
          l.getSide(),
          l.getAmount(),
          l.getPartyCode(),
          l.getInvoiceNo(),
          l.getComponent(),
          l.getCostCenter(),
          l.getBusinessLine(),
          l.getNarration(),
          l.getOrigin(),
          l.getOriginalBatchNo(),
          l.getOriginalLineNo());
    }
  }

  /**
   * A correction in a list.
   *
   * @param id id
   * @param correctionNo number
   * @param kind kind
   * @param stage stage
   * @param invoiceNo invoice
   * @param description description
   * @param currency currency
   * @param journalBatchNo posted journal
   * @param createdBy raised by
   * @param createdAt raised at
   */
  record CorrectionSummary(
      Long id,
      String correctionNo,
      String kind,
      CorrectionStage stage,
      String invoiceNo,
      String description,
      String currency,
      String journalBatchNo,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a correction (without its lines).
     *
     * @param c correction
     * @return summary
     */
    public static CorrectionSummary from(Correction c) {
      return new CorrectionSummary(
          c.getId(),
          c.getCorrectionNo(),
          c.getKind(),
          c.getStage(),
          c.getInvoiceNo(),
          c.getDescription(),
          c.getCurrency(),
          c.getJournalBatchNo(),
          c.getCreatedBy(),
          c.getCreatedAt());
    }
  }

  /**
   * A correction with its lines.
   *
   * @param id id
   * @param correctionNo number
   * @param caseId case it was raised from
   * @param kind kind
   * @param stage stage
   * @param invoiceNo invoice
   * @param rootInvoiceNo root invoice
   * @param originalBatchNo corrected journal
   * @param currency currency
   * @param description description
   * @param trail submitted, reviewed and approved by / at
   * @param returnComment last return comment
   * @param journalBatchNo posted journal
   * @param postedAt posted at
   * @param openItems open items recorded
   * @param ledgerMovements invoice ledger movements recorded
   * @param totalDebit total debit
   * @param totalCredit total credit
   * @param lines lines
   * @param createdBy raised by
   */
  record CorrectionView(
      Long id,
      String correctionNo,
      Long caseId,
      String kind,
      CorrectionStage stage,
      String invoiceNo,
      String rootInvoiceNo,
      String originalBatchNo,
      String currency,
      String description,
      Trail trail,
      String returnComment,
      String journalBatchNo,
      Instant postedAt,
      int openItems,
      int ledgerMovements,
      BigDecimal totalDebit,
      BigDecimal totalCredit,
      List<LineView> lines,
      String createdBy) {

    /**
     * Maps a correction.
     *
     * @param c correction (lines loaded)
     * @return view
     */
    public static CorrectionView from(Correction c) {
      return new CorrectionView(
          c.getId(),
          c.getCorrectionNo(),
          c.getCaseId(),
          c.getKind(),
          c.getStage(),
          c.getInvoiceNo(),
          c.getRootInvoiceNo(),
          c.getOriginalBatchNo(),
          c.getCurrency(),
          c.getDescription(),
          new Trail(
              c.getSubmittedBy(),
              c.getSubmittedAt(),
              c.getReviewedBy(),
              c.getReviewedAt(),
              c.getApprovedBy(),
              c.getApprovedAt()),
          c.getReturnComment(),
          c.getJournalBatchNo(),
          c.getPostedAt(),
          c.getOpenItems(),
          c.getLedgerMovements(),
          c.totalDebit(),
          c.totalCredit(),
          c.getLines().stream().map(LineView::from).toList(),
          c.getCreatedBy());
    }
  }

  /**
   * Who moved a correction.
   *
   * @param submittedBy preparer
   * @param submittedAt submitted at
   * @param reviewedBy reviewer
   * @param reviewedAt reviewed at
   * @param approvedBy approver
   * @param approvedAt approved at
   */
  record Trail(
      String submittedBy,
      Instant submittedAt,
      String reviewedBy,
      Instant reviewedAt,
      String approvedBy,
      Instant approvedAt) {}
}
