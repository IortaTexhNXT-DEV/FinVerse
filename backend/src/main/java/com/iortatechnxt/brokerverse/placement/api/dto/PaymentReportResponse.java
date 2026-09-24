package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.domain.MatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReport;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A payment report and its match review (BRNB.067/068).
 *
 * @param id id
 * @param reportNo report number
 * @param kind CLPC or REFERENCE
 * @param batchId billing batch answered
 * @param fileName uploaded file
 * @param status REVIEW, CONFIRMED or DISCARDED
 * @param matched lines matched as paid
 * @param unpaid lines matched but unpaid
 * @param unmatched lines without an account
 * @param ambiguous lines with several accounts
 * @param confirmedBy confirmed by
 * @param confirmedAt confirmed at
 * @param createdAt uploaded at
 * @param createdBy uploaded by
 * @param lines lines (detail only)
 */
public record PaymentReportResponse(
    Long id,
    String reportNo,
    String kind,
    Long batchId,
    String fileName,
    String status,
    long matched,
    long unpaid,
    long unmatched,
    long ambiguous,
    String confirmedBy,
    Instant confirmedAt,
    Instant createdAt,
    String createdBy,
    List<Line> lines) {

  /**
   * Maps a report without its lines.
   *
   * @param r report
   * @return response
   */
  public static PaymentReportResponse from(PaymentReport r) {
    return of(r, List.of());
  }

  /**
   * Maps a report with its lines.
   *
   * @param r report
   * @return response
   */
  public static PaymentReportResponse detail(PaymentReport r) {
    return of(r, r.getLines().stream().map(Line::from).toList());
  }

  private static PaymentReportResponse of(PaymentReport r, List<Line> lines) {
    return new PaymentReportResponse(
        r.getId(),
        r.getReportNo(),
        r.getKind().name(),
        r.getBatchId(),
        r.getFileName(),
        r.getStatus().name(),
        r.count(MatchStatus.MATCHED),
        r.count(MatchStatus.UNPAID),
        r.count(MatchStatus.UNMATCHED),
        r.count(MatchStatus.AMBIGUOUS),
        r.getConfirmedBy(),
        r.getConfirmedAt(),
        r.getCreatedAt(),
        r.getCreatedBy(),
        lines);
  }

  /**
   * A report line.
   *
   * @param id id
   * @param rowNo row in the file
   * @param reference reported reference
   * @param paid reported paid
   * @param amount amount
   * @param paidOn payment date
   * @param matchStatus match outcome
   * @param accountId matched account
   * @param arn matched ARN
   * @param candidates candidate ARNs (ambiguous)
   * @param message explanation
   * @param manuallyMatched chosen by the user
   * @param applied payment gate opened
   * @param applyMessage outcome of the confirmation
   */
  public record Line(
      Long id,
      int rowNo,
      String reference,
      boolean paid,
      BigDecimal amount,
      LocalDate paidOn,
      String matchStatus,
      Long accountId,
      String arn,
      String candidates,
      String message,
      boolean manuallyMatched,
      boolean applied,
      String applyMessage) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static Line from(PaymentReportLine l) {
      return new Line(
          l.getId(),
          l.getRowNo(),
          l.getReference(),
          l.isPaid(),
          l.getAmount(),
          l.getPaidOn(),
          l.getMatchStatus().name(),
          l.getAccountId(),
          l.getArn(),
          l.getCandidates(),
          l.getMessage(),
          l.isManuallyMatched(),
          l.isApplied(),
          l.getApplyMessage());
    }
  }
}
