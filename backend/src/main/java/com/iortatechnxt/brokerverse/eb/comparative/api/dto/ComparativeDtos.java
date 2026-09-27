package com.iortatechnxt.brokerverse.eb.comparative.api.dto;

import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeMatrix;
import com.iortatechnxt.brokerverse.eb.domain.EbComment;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeSignoff;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the comparative API (FR-EB-041 to 043). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ComparativeDtos {

  private ComparativeDtos() {}

  /**
   * The recommended proposal per benefit line and the summary.
   *
   * @param recommendation proposal id per benefit line
   * @param summary summary
   */
  public record RecommendationRequest(Map<String, Long> recommendation, String summary) {}

  /**
   * Remarks or a reason.
   *
   * @param remarks remarks or reason
   */
  public record RemarksRequest(String remarks) {}

  /**
   * A comment.
   *
   * @param text text
   * @param client whether it is the client's comment
   * @param replyTo comment answered
   */
  public record CommentRequest(String text, boolean client, Long replyTo) {}

  /**
   * A comparative in a list.
   *
   * @param id id
   * @param comparativeNo number
   * @param cycleId cycle
   * @param versionNo version
   * @param status status
   * @param dueDate due to the client
   * @param submittedBy maker
   * @param presentedAt presented at
   * @param thresholdRules rules met
   */
  public record ComparativeSummary(
      Long id,
      String comparativeNo,
      Long cycleId,
      int versionNo,
      String status,
      LocalDate dueDate,
      String submittedBy,
      Instant presentedAt,
      String thresholdRules) {

    /**
     * Maps a comparative.
     *
     * @param c comparative
     * @return summary
     */
    public static ComparativeSummary from(EbComparative c) {
      return new ComparativeSummary(
          c.getId(),
          c.getComparativeNo(),
          c.getCycleId(),
          c.getVersionNo(),
          c.getStatus().name(),
          c.getDueDate(),
          c.maker(),
          c.getPresentedAt(),
          c.getThresholdRules());
    }
  }

  /**
   * The comparative page.
   *
   * @param comparative summary
   * @param programmeId programme
   * @param programmeNo programme number
   * @param programmeName programme name
   * @param clientName client
   * @param accountOfficer account officer
   * @param cycleNo cycle number
   * @param cycleStage cycle stage
   * @param summary AO's summary
   * @param approverPermission threshold approver permission
   * @param attachmentId PDF sent to the client
   * @param lines recommendation per benefit line
   * @param matrix rows
   * @param decisions sign-offs and returns
   * @param comments comment thread
   */
  public record ComparativeView(
      ComparativeSummary comparative,
      Long programmeId,
      String programmeNo,
      String programmeName,
      String clientName,
      String accountOfficer,
      String cycleNo,
      String cycleStage,
      String summary,
      String approverPermission,
      Long attachmentId,
      List<LineView> lines,
      ComparativeMatrix matrix,
      List<DecisionView> decisions,
      List<CommentView> comments) {}

  /**
   * A benefit line with its recommendation.
   *
   * @param benefitLine benefit line
   * @param recommendedProposalId recommended proposal
   * @param lowestPremium lowest premium
   */
  public record LineView(String benefitLine, Long recommendedProposalId, BigDecimal lowestPremium) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static LineView from(EbComparative.Line l) {
      return new LineView(l.getBenefitLine(), l.getRecommendedProposalId(), l.getLowestPremium());
    }
  }

  /**
   * A decision.
   *
   * @param role SIGNOFF or THRESHOLD
   * @param signatory user
   * @param decision APPROVED or RETURNED
   * @param remarks remarks
   * @param decidedAt time
   */
  public record DecisionView(
      String role, String signatory, String decision, String remarks, Instant decidedAt) {

    /**
     * Maps a decision.
     *
     * @param s decision
     * @return view
     */
    public static DecisionView from(EbComparativeSignoff s) {
      return new DecisionView(
          s.getRole(), s.getSignatory(), s.getDecision(), s.getRemarks(), s.getDecidedAt());
    }
  }

  /**
   * A comment.
   *
   * @param id id
   * @param authorKind INTERNAL or CLIENT
   * @param text text
   * @param replyToId comment answered
   * @param createdBy author (the AO for a client comment)
   * @param createdAt time
   */
  public record CommentView(
      Long id,
      String authorKind,
      String text,
      Long replyToId,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a comment.
     *
     * @param c comment
     * @return view
     */
    public static CommentView from(EbComment c) {
      return new CommentView(
          c.getId(),
          c.getAuthorKind(),
          c.getText(),
          c.getReplyToId(),
          c.getCreatedBy(),
          c.getCreatedAt());
    }
  }
}
