package com.iortatechnxt.brokerverse.productmaint.api.dto;

import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageResponseHistory;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.RoundStatus;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeTable;
import com.iortatechnxt.brokerverse.productmaint.service.PackageResponseService.ResponseTerms;
import com.iortatechnxt.brokerverse.productmaint.service.TermsService.InsurerChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** Request and response bodies of the negotiation endpoints (BRPM.010/012-014, PMADD04). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class NegotiationDtos {

  private NegotiationDtos() {}

  /**
   * Insurers and notes of a round's slip.
   *
   * @param insurers insurer codes
   * @param notes quotation slip notes
   */
  public record PrepareBody(List<String> insurers, @Size(max = 4000) String notes) {}

  /**
   * Submission of a slip.
   *
   * @param replyBy reply date, null for the configured delay
   */
  public record SlipSubmitBody(LocalDate replyBy) {}

  /**
   * The terms keyed in for an insurer.
   *
   * @param outcome outcome (list PKG_RESPONSE_OUTCOME)
   * @param rate rate in percent
   * @param minimumPremium minimum premium
   * @param coverages terms per coverage
   * @param conditions conditions and warranties
   * @param validUntil validity
   * @param remarks remarks
   */
  public record ResponseBody(
      @NotBlank @Size(max = 30) String outcome,
      BigDecimal rate,
      BigDecimal minimumPremium,
      List<CoverageTerm> coverages,
      @Size(max = 2000) String conditions,
      LocalDate validUntil,
      @Size(max = 1000) String remarks) {

    /**
     * The service input.
     *
     * @return terms
     */
    public ResponseTerms toTerms() {
      return new ResponseTerms(
          outcome, rate, minimumPremium, coverages, conditions, validUntil, remarks);
    }
  }

  /**
   * Terms final: the chosen insurers.
   *
   * @param insurers chosen insurers with role and share
   * @param comment comment
   */
  public record TermsFinalBody(List<InsurerChoice> insurers, @Size(max = 1000) String comment) {}

  /**
   * A negotiation round with its responses.
   *
   * @param id id
   * @param roundNo round number
   * @param status slip status
   * @param qsNo QS number
   * @param qsTemplate template version
   * @param qsNotes notes
   * @param replyDue reply date
   * @param preparedBy preparer
   * @param approvedBy approver
   * @param sentAt sent
   * @param locked locked since the ManCom sign-off
   * @param insurers insurers approached
   * @param responses responses
   */
  public record RoundView(
      Long id,
      int roundNo,
      RoundStatus status,
      String qsNo,
      String qsTemplate,
      String qsNotes,
      LocalDate replyDue,
      String preparedBy,
      String approvedBy,
      Instant sentAt,
      boolean locked,
      List<String> insurers,
      List<ResponseView> responses) {

    /**
     * Maps a round.
     *
     * @param r round
     * @param responses its responses
     * @return view
     */
    public static RoundView from(NegotiationRound r, List<ResponseView> responses) {
      return new RoundView(
          r.getId(),
          r.getRoundNo(),
          r.getStatus(),
          r.getQsNo(),
          r.getQsTemplate(),
          r.getQsNotes(),
          r.getReplyDue(),
          r.getPreparedBy(),
          r.getApprovedBy(),
          r.getSentAt(),
          r.isLocked(),
          r.getInsurers(),
          responses);
    }
  }

  /**
   * An insurer response.
   *
   * @param id id
   * @param roundId round
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param outcome outcome
   * @param revision revision
   * @param rate rate
   * @param minimumPremium minimum premium
   * @param coverages terms per coverage
   * @param conditions conditions
   * @param validUntil validity
   * @param remarks remarks
   * @param documentId response document
   * @param respondedAt last keyed in
   * @param sends number of slip e-mails sent
   */
  public record ResponseView(
      Long id,
      Long roundId,
      String insurerCode,
      String insurerName,
      String outcome,
      int revision,
      BigDecimal rate,
      BigDecimal minimumPremium,
      List<CoverageTerm> coverages,
      String conditions,
      LocalDate validUntil,
      String remarks,
      Long documentId,
      Instant respondedAt,
      int sends) {

    /**
     * Maps a response.
     *
     * @param r response
     * @param terms reads its coverage terms
     * @return view
     */
    public static ResponseView from(
        PackageInsurerResponse r, Function<PackageInsurerResponse, List<CoverageTerm>> terms) {
      return new ResponseView(
          r.getId(),
          r.getRoundId(),
          r.getInsurerCode(),
          r.getInsurerName(),
          r.getOutcome(),
          r.getRevision(),
          r.getRate(),
          r.getMinimumPremium(),
          terms.apply(r),
          r.getConditions(),
          r.getValidUntil(),
          r.getRemarks(),
          r.getResponseDocumentId(),
          r.getRespondedAt(),
          r.getSends());
    }
  }

  /**
   * A revision of a response.
   *
   * @param responseId response
   * @param revision revision
   * @param outcome outcome
   * @param rate rate
   * @param minimumPremium minimum premium
   * @param conditions conditions
   * @param remarks remarks
   * @param changedBy who
   * @param changedAt when
   */
  public record HistoryView(
      Long responseId,
      int revision,
      String outcome,
      BigDecimal rate,
      BigDecimal minimumPremium,
      String conditions,
      String remarks,
      String changedBy,
      Instant changedAt) {

    /**
     * Maps a snapshot.
     *
     * @param h snapshot
     * @return view
     */
    public static HistoryView from(PackageResponseHistory h) {
      return new HistoryView(
          h.getResponseId(),
          h.getRevision(),
          h.getOutcome(),
          h.getRate(),
          h.getMinimumPremium(),
          h.getConditions(),
          h.getRemarks(),
          h.getChangedBy(),
          h.getChangedAt());
    }
  }

  /**
   * A comparative table as shown on screen.
   *
   * @param roundNo round
   * @param fields field codes shown
   * @param headers column headers
   * @param rows rows
   */
  public record ComparativeView(
      int roundNo, List<String> fields, List<String> headers, List<ComparativeTable.Row> rows) {

    /**
     * Maps a table.
     *
     * @param t table
     * @return view
     */
    public static ComparativeView from(ComparativeTable t) {
      return new ComparativeView(t.roundNo(), t.fields(), t.headers(), t.rows());
    }
  }
}
