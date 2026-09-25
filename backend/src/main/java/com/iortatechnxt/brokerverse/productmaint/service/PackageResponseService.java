package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRoundRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse.ResponseInput;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageResponseHistory;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageResponseHistoryRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageResponseRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RoundStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer responses of the negotiation rounds (BRPM.013, PMADD04): TSU keys in each insurer's
 * outcome (including the exception states Approved with Changes, Counter-proposal, Declined, No
 * Response) and structured terms per coverage, and attaches the response document; every change is
 * kept in the revision history and audited. The live comparative table of a round is compiled from
 * the responses.
 */
@Service
@Transactional
public class PackageResponseService {

  /** Document type of an insurer's package terms. */
  public static final String RESPONSE_DOCUMENT = "PKG_INSURER_RESPONSE";

  private static final String RESPONSE = "Insurer response";

  private final PackageRequests requests;
  private final NegotiationRoundRepository rounds;
  private final PackageResponseRepository responses;
  private final PackageResponseHistoryRepository history;
  private final TermsCodec codec;
  private final LovService lovs;
  private final DocumentService documents;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param rounds negotiation rounds
   * @param responses insurer responses
   * @param history response history
   * @param codec terms JSON
   * @param lovs lists of values (outcomes)
   * @param documents documents of the request
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PackageResponseService(
      PackageRequests requests,
      NegotiationRoundRepository rounds,
      PackageResponseRepository responses,
      PackageResponseHistoryRepository history,
      TermsCodec codec,
      LovService lovs,
      DocumentService documents,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.rounds = rounds;
    this.responses = responses;
    this.history = history;
    this.codec = codec;
    this.lovs = lovs;
    this.documents = documents;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Responses of every round of a request.
   *
   * @param id request
   * @return responses, first round first
   */
  @Transactional(readOnly = true)
  public List<PackageInsurerResponse> responses(Long id) {
    List<Long> roundIds =
        rounds.findByRequestIdOrderByRoundNo(requests.get(id).getId()).stream()
            .map(NegotiationRound::getId)
            .toList();
    return roundIds.isEmpty() ? List.of() : responses.findByRoundIdInOrderById(roundIds);
  }

  /**
   * Responses of one round.
   *
   * @param round round
   * @return responses
   */
  @Transactional(readOnly = true)
  public List<PackageInsurerResponse> ofRound(NegotiationRound round) {
    return responses.findByRoundIdOrderById(round.getId());
  }

  /**
   * Revision history of the responses of a request, newest first.
   *
   * @param id request
   * @return snapshots
   */
  @Transactional(readOnly = true)
  public List<PackageResponseHistory> history(Long id) {
    List<Long> ids = responses(id).stream().map(PackageInsurerResponse::getId).toList();
    return ids.isEmpty() ? List.of() : history.findByResponseIdInOrderByIdDesc(ids);
  }

  /**
   * The coverage terms of a response.
   *
   * @param response response
   * @return terms per coverage
   */
  public List<CoverageTerm> terms(PackageInsurerResponse response) {
    return codec.coverages(response.getTerms());
  }

  /**
   * The live comparative table of a round (BRPM.014).
   *
   * @param round round
   * @return table
   */
  @Transactional(readOnly = true)
  public ComparativeTable comparative(NegotiationRound round) {
    return ComparativeTable.compile(round.getRoundNo(), ofRound(round), codec::coverages);
  }

  /**
   * Records an insurer's outcome and terms (TSU, while the round is open).
   *
   * @param id request
   * @param responseId response
   * @param terms outcome and terms
   * @return the response
   */
  public PackageInsurerResponse record(Long id, Long responseId, ResponseTerms terms) {
    PackageInsurerResponse response = open(id, responseId);
    lovs.requireValid("PKG_RESPONSE_OUTCOME", terms.outcome(), LocalDate.now(clock));
    if (ComparativeTable.offered(terms.outcome()) && terms.rate() == null) {
      throw new BusinessRuleException(
          "PKG_RESPONSE_RATE_REQUIRED",
          "Enter the rate offered by " + response.getInsurerName() + " (" + terms.outcome() + ")");
    }
    if (negative(terms.rate()) || negative(terms.minimumPremium())) {
      throw new BusinessRuleException(
          "PKG_AMOUNT_NEGATIVE", "Rates and amounts cannot be negative");
    }
    response.record(
        new ResponseInput(
            terms.outcome(),
            terms.rate(),
            terms.minimumPremium(),
            codec.coveragesJson(terms.coverages()),
            terms.conditions(),
            terms.validUntil(),
            terms.remarks()),
        clock.instant());
    snapshot(id, response, "Terms of " + response.getInsurerName() + ": " + terms.outcome());
    return response;
  }

  /**
   * Attaches an insurer's response document to the request and links it to the response.
   *
   * @param id request
   * @param responseId response
   * @param file uploaded file
   * @return the response
   */
  public PackageInsurerResponse attach(Long id, Long responseId, UploadedFile file) {
    PackageInsurerResponse response = open(id, responseId);
    PackageRequest p = requests.get(id);
    Attachment saved =
        documents
            .upload(
                new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(id)),
                List.of(file),
                new UploadOptions(
                    RESPONSE_DOCUMENT,
                    true,
                    p.getRequestNo(),
                    "Terms of " + response.getInsurerName()))
            .get(0);
    response.attachDocument(saved.getId());
    snapshot(id, response, "Response document of " + response.getInsurerName());
    return response;
  }

  private PackageInsurerResponse open(Long id, Long responseId) {
    requests.inStage(
        id,
        RequestStage.NEGOTIATION,
        "PKG_NOT_IN_NEGOTIATION",
        "Insurer terms are keyed in during the negotiation");
    PackageInsurerResponse response =
        responses
            .findById(responseId)
            .orElseThrow(() -> new ResourceNotFoundException(RESPONSE, responseId));
    NegotiationRound round =
        rounds
            .findById(response.getRoundId())
            .filter(r -> r.getRequestId().equals(id))
            .orElseThrow(() -> new ResourceNotFoundException(RESPONSE, responseId));
    if (round.isLocked() || round.getStatus() != RoundStatus.SENT) {
      throw new BusinessRuleException(
          "PKG_ROUND_CLOSED",
          "Round " + round.getRoundNo() + " is closed: key in the latest round");
    }
    return response;
  }

  private void snapshot(Long id, PackageInsurerResponse response, String summary) {
    history.save(new PackageResponseHistory(response, currentUser.username(), clock.instant()));
    audit.record(
        PackageRequests.ENTITY, requests.get(id).getRequestNo(), AuditAction.UPDATE, summary);
  }

  private static boolean negative(BigDecimal value) {
    return value != null && value.signum() < 0;
  }

  /**
   * What TSU keys in for an insurer (PMADD04).
   *
   * @param outcome outcome (list PKG_RESPONSE_OUTCOME)
   * @param rate rate in percent (mandatory for an offer)
   * @param minimumPremium minimum premium
   * @param coverages terms per coverage
   * @param conditions conditions and warranties
   * @param validUntil validity
   * @param remarks remarks
   */
  public record ResponseTerms(
      String outcome,
      BigDecimal rate,
      BigDecimal minimumPremium,
      List<CoverageTerm> coverages,
      String conditions,
      LocalDate validUntil,
      String remarks) {

    /** Defensive copy. */
    public ResponseTerms {
      coverages = coverages == null ? List.of() : List.copyOf(coverages);
    }
  }
}
