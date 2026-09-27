package com.iortatechnxt.brokerverse.eb.market.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import com.iortatechnxt.brokerverse.eb.market.service.RevisionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the TOR, insurer request, proposal and revision API. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class MarketDtos {

  private MarketDtos() {}

  /**
   * The items of the draft TOR.
   *
   * @param items items in order
   */
  public record TorItemsRequest(List<EbTorItem.Data> items) {}

  /**
   * A TOR version.
   *
   * @param id id
   * @param versionNo version
   * @param status status
   * @param releasedAt released at
   * @param releasedBy released by
   * @param attachmentId the TOR PDF
   * @param items items
   */
  public record TorResponse(
      Long id,
      int versionNo,
      String status,
      Instant releasedAt,
      String releasedBy,
      Long attachmentId,
      List<TorItemResponse> items) {

    /**
     * Maps a version.
     *
     * @param t version
     * @return response
     */
    public static TorResponse from(EbTor t) {
      return new TorResponse(
          t.getId(),
          t.getVersionNo(),
          t.getStatus().name(),
          t.getReleasedAt(),
          t.getReleasedBy(),
          t.getAttachmentId(),
          t.getItems().stream().map(TorItemResponse::from).toList());
    }
  }

  /**
   * A TOR item.
   *
   * @param id id
   * @param sortOrder order
   * @param benefitLine benefit line
   * @param planCode plan
   * @param description item
   * @param requirement requirement
   */
  public record TorItemResponse(
      Long id,
      int sortOrder,
      String benefitLine,
      String planCode,
      String description,
      String requirement) {

    static TorItemResponse from(EbTorItem i) {
      return new TorItemResponse(
          i.getId(),
          i.getSortOrder(),
          i.getBenefitLine(),
          i.getPlanCode(),
          i.getDescription(),
          i.getRequirement());
    }
  }

  /**
   * Insurers to send requests to.
   *
   * @param insurerCodes insurers
   */
  public record InsurersRequest(List<String> insurerCodes) {}

  /**
   * Closes a request.
   *
   * @param declined whether the insurer declined
   * @param reason why
   */
  public record CloseRequest(boolean declined, String reason) {}

  /**
   * An insurer request.
   *
   * @param id id
   * @param requestNo number
   * @param cycleId cycle
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param torVersion TOR version sent
   * @param sentAt sent at
   * @param sentBy sent by
   * @param dueDate reply due
   * @param status status
   * @param closedReason why closed
   */
  public record RequestResponse(
      Long id,
      String requestNo,
      Long cycleId,
      String insurerCode,
      String insurerName,
      int torVersion,
      Instant sentAt,
      String sentBy,
      LocalDate dueDate,
      String status,
      String closedReason) {

    /**
     * Maps a request.
     *
     * @param r request
     * @param insurerName insurer name
     * @return response
     */
    public static RequestResponse from(EbInsurerRequest r, String insurerName) {
      return new RequestResponse(
          r.getId(),
          r.getRequestNo(),
          r.getCycleId(),
          r.getInsurerCode(),
          insurerName,
          r.getTorVersion(),
          r.getSentAt(),
          r.getSentBy(),
          r.getDueDate(),
          r.getStatus().name(),
          r.getClosedReason());
    }
  }

  /**
   * A proposal as entered (JSON field of the multipart request).
   *
   * @param insurerCode insurer
   * @param receivedOn date received
   * @param validUntil validity
   * @param currency currency
   * @param terms terms
   * @param exclusions exclusions
   * @param lines premium per line and plan
   * @param items answers to the TOR items
   * @param factors capability factors
   */
  public record ProposalRequest(
      String insurerCode,
      LocalDate receivedOn,
      LocalDate validUntil,
      String currency,
      String terms,
      String exclusions,
      List<EbProposalLine.Data> lines,
      List<EbProposalItem.Data> items,
      List<EbProposalFactor.Data> factors) {}

  /**
   * A reason.
   *
   * @param reason why
   */
  public record ReasonRequest(String reason) {}

  /**
   * A proposal.
   *
   * @param id id
   * @param proposalNo number
   * @param cycleId cycle
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param kind kind
   * @param versionNo version
   * @param status status
   * @param receivedOn received on
   * @param validUntil validity
   * @param currency currency
   * @param terms terms
   * @param exclusions exclusions
   * @param attachmentId the insurer's document
   * @param totalPremium annual premium of every line
   * @param rejectReason rejection reason
   * @param decidedBy validated or rejected by
   * @param lines plans
   * @param items answers
   * @param factors capability factors
   */
  public record ProposalResponse(
      Long id,
      String proposalNo,
      Long cycleId,
      String insurerCode,
      String insurerName,
      String kind,
      int versionNo,
      String status,
      LocalDate receivedOn,
      LocalDate validUntil,
      String currency,
      String terms,
      String exclusions,
      Long attachmentId,
      BigDecimal totalPremium,
      String rejectReason,
      String decidedBy,
      List<EbProposalLine.Data> lines,
      List<EbProposalItem.Data> items,
      List<EbProposalFactor.Data> factors) {

    /**
     * Maps a proposal.
     *
     * @param p proposal
     * @param insurerName insurer name
     * @return response
     */
    public static ProposalResponse from(EbProposal p, String insurerName) {
      return new ProposalResponse(
          p.getId(),
          p.getProposalNo(),
          p.getCycleId(),
          p.getInsurerCode(),
          insurerName,
          p.getKind().name(),
          p.getVersionNo(),
          p.getStatus().name(),
          p.getReceivedOn(),
          p.getValidUntil(),
          p.getCurrency(),
          p.getTerms(),
          p.getExclusions(),
          p.getAttachmentId(),
          p.getLines().stream()
              .map(EbProposalLine::getAnnualPremium)
              .reduce(BigDecimal.ZERO, BigDecimal::add),
          p.getRejectReason(),
          p.getDecidedBy(),
          p.getLines().stream()
              .map(
                  l ->
                      new EbProposalLine.Data(
                          l.getBenefitLine(),
                          l.getPlanCode(),
                          l.getPlanName(),
                          l.getMembers(),
                          l.getPremiumRate(),
                          l.getAnnualPremium(),
                          l.getSumInsured()))
              .toList(),
          p.getItems().stream()
              .map(
                  i ->
                      new EbProposalItem.Data(
                          i.getTorItemId(), i.getOfferedValue(), i.isDeviation(), i.getRemark()))
              .toList(),
          p.getFactors().stream()
              .map(
                  f ->
                      new EbProposalFactor.Data(
                          f.getFactorCode(), f.getFactorValue(), f.getRating()))
              .toList());
    }
  }

  /**
   * The client's changes to relay.
   *
   * @param description description
   * @param changes requested changes
   * @param insurerCodes insurers
   */
  public record RevisionRequestBody(
      String description, List<RevisionService.Change> changes, List<String> insurerCodes) {}

  /**
   * A revision request.
   *
   * @param id id
   * @param revisionNo number within the cycle
   * @param description description
   * @param relayedAt relayed at
   * @param relayedBy relayed by
   * @param dueDate due
   * @param status status
   * @param items requested changes
   * @param targets insurers and their status
   */
  public record RevisionResponse(
      Long id,
      int revisionNo,
      String description,
      Instant relayedAt,
      String relayedBy,
      LocalDate dueDate,
      String status,
      List<RevisionService.Change> items,
      List<Target> targets) {

    /**
     * Maps a revision.
     *
     * @param r revision
     * @return response
     */
    public static RevisionResponse from(EbRevisionRequest r) {
      return new RevisionResponse(
          r.getId(),
          r.getRevisionNo(),
          r.getDescription(),
          r.getRelayedAt(),
          r.getRelayedBy(),
          r.getDueDate(),
          r.getStatus().name(),
          r.getItems().stream()
              .map(i -> new RevisionService.Change(i.getTorItemId(), i.getRequestedChange()))
              .toList(),
          r.getTargets().stream()
              .map(
                  t ->
                      new Target(
                          t.getInsurerCode(), t.getStatus().name(), t.getAnsweredProposalId()))
              .toList());
    }
  }

  /**
   * An insurer of a revision.
   *
   * @param insurerCode insurer
   * @param status OPEN or ANSWERED
   * @param answeredProposalId revised proposal
   */
  public record Target(String insurerCode, String status, Long answeredProposalId) {}
}
