package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RoundStatus;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The outcome of the negotiation (BRPM.010/013): terms final (every insurer of the latest round has
 * an outcome, the chosen insurers offered terms, co-insurance shares add up to 100), which copies
 * the negotiated terms into the request's proposed terms and compiles the comparative master; the
 * TSU Head's release of the terms to Marketing (client-specific packages) or its direct move to the
 * requirements (generic programmes); and Marketing's acceptance.
 */
@Service
@Transactional
public class TermsService {

  private static final Set<String> ROLES = Set.of("LEAD", "PARTICIPANT", "PANEL");
  private static final BigDecimal FULL_SHARE = BigDecimal.valueOf(100);

  private final PackageRequests requests;
  private final NegotiationService negotiation;
  private final PackageResponseService responses;
  private final ComparativeService comparatives;
  private final TermsCodec codec;
  private final WorkflowService workflow;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param negotiation rounds
   * @param responses insurer responses
   * @param comparatives comparative outputs
   * @param codec terms JSON
   * @param workflow workflow engine
   * @param currentUser current user
   * @param clock clock
   */
  public TermsService(
      PackageRequests requests,
      NegotiationService negotiation,
      PackageResponseService responses,
      ComparativeService comparatives,
      TermsCodec codec,
      WorkflowService workflow,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.negotiation = negotiation;
    this.responses = responses;
    this.comparatives = comparatives;
    this.codec = codec;
    this.workflow = workflow;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Terms final (BRPM.010): checks the latest round, sets the proposed terms from the chosen
   * insurers' responses, compiles the comparative master and moves the request to TERMS_REVIEW.
   *
   * @param id request
   * @param chosen chosen insurers with their role and share
   * @param comment comment
   * @return the request
   */
  public PackageRequest termsFinal(Long id, List<InsurerChoice> chosen, String comment) {
    PackageRequest p =
        requests.inStage(
            id,
            RequestStage.NEGOTIATION,
            "PKG_NOT_IN_NEGOTIATION",
            "Terms are made final during the negotiation");
    NegotiationRound round = negotiation.latest(id);
    List<PackageInsurerResponse> all = responses.ofRound(round);
    long pending = all.stream().filter(r -> !r.isAnswered()).count();
    if (round.getStatus() != RoundStatus.SENT || all.isEmpty() || pending > 0) {
      throw new BusinessRuleException(
          "NEGOTIATION_INCOMPLETE",
          "Every insurer of round "
              + round.getRoundNo()
              + " needs an outcome before the terms are final ("
              + pending
              + " pending)");
    }
    List<InsurerLine> lines = chosenLines(chosen, all);
    PackageTerms requested = codec.terms(p.getRequestedTerms());
    PackageTerms proposed = requested.withInsurers(lines);
    p.propose(codec.toJson(proposed), proposed.insurerCodes());
    p.summarise(proposed.dates().packageEndDate(), proposed.scheme().defaultRate());
    comparatives.compileMaster(id);
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), "terms_final", TransitionNote.comment(comment));
    p.getMilestones().termsFinal(currentUser.username(), clock.instant());
    return p;
  }

  private List<InsurerLine> chosenLines(
      List<InsurerChoice> chosen, List<PackageInsurerResponse> all) {
    if (chosen == null || chosen.isEmpty()) {
      throw new BusinessRuleException(
          "NEGOTIATION_INCOMPLETE", "Choose the insurer(s) whose terms the package takes");
    }
    Map<String, PackageInsurerResponse> byCode =
        all.stream()
            .collect(Collectors.toMap(PackageInsurerResponse::getInsurerCode, Function.identity()));
    List<InsurerLine> lines = new ArrayList<>();
    for (InsurerChoice c : chosen) {
      PackageInsurerResponse r = byCode.get(c.insurerCode());
      if (r == null || !ComparativeTable.offered(r.getOutcome())) {
        throw new BusinessRuleException(
            "PKG_INSURER_NOT_OFFERED", c.insurerCode() + " did not offer terms in this round");
      }
      lines.add(
          new InsurerLine(
              r.getInsurerCode(),
              role(c.role()),
              c.sharePercent(),
              r.getRate(),
              r.getMinimumPremium(),
              responses.terms(r)));
    }
    checkShares(lines);
    return lines;
  }

  private static String role(String given) {
    String role = given == null ? "PANEL" : given.strip().toUpperCase(Locale.ROOT);
    if (!ROLES.contains(role)) {
      throw new BusinessRuleException("PKG_INSURER_ROLE_INVALID", "Unknown role " + given);
    }
    return role;
  }

  private static void checkShares(List<InsurerLine> lines) {
    List<BigDecimal> shares =
        lines.stream().map(InsurerLine::sharePercent).filter(Objects::nonNull).toList();
    if (shares.isEmpty()) {
      return;
    }
    BigDecimal total = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (shares.size() != lines.size() || total.compareTo(FULL_SHARE) != 0) {
      throw new BusinessRuleException(
          "PKG_SHARES_INVALID",
          "Co-insurance shares must be given for every insurer and total 100");
    }
  }

  /**
   * TSU Head: releases the negotiated terms to Marketing (client-specific package, BRPM.013).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest releaseToMarketing(Long id, String comment) {
    PackageRequest p = requests.get(id);
    if (p.getScope() != RequestScope.CLIENT_SPECIFIC) {
      throw new BusinessRuleException(
          "PKG_SCOPE_GENERIC", "A generic programme goes straight to the requirements");
    }
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "release_to_marketing",
        TransitionNote.comment(comment));
    return p;
  }

  /**
   * TSU Head: moves a generic programme's terms straight to the requirements.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest skipMarketingReview(Long id, String comment) {
    PackageRequest p = requests.get(id);
    if (p.getScope() != RequestScope.GENERIC) {
      throw new BusinessRuleException(
          "PKG_SCOPE_CLIENT", "Marketing reviews the terms of a client-specific package");
    }
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "skip_marketing_review",
        TransitionNote.comment(comment));
    return p;
  }

  /**
   * Marketing accepts the negotiated terms of a client-specific package (BRPM.013).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest acceptTerms(Long id, String comment) {
    PackageRequest p = requests.get(id);
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "accept_terms",
        TransitionNote.comment(comment));
    return p;
  }

  /**
   * An insurer chosen at terms final.
   *
   * @param insurerCode insurer
   * @param role LEAD, PARTICIPANT or PANEL (null = PANEL)
   * @param sharePercent co-insurance share, null for a panel insurer
   */
  public record InsurerChoice(String insurerCode, String role, BigDecimal sharePercent) {}
}
