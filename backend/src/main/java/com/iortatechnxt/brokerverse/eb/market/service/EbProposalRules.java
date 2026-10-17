package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRevisionRequest;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks of a proposal entered by the AO: the premium lines (benefit line, plan, amounts), the
 * answers to the TOR items, the capability factors, the dates and, for a revised proposal, an
 * answer to every requested change on a TOR item (FR-EB-045 R1).
 */
@Component
public class EbProposalRules {

  private static final int MAX_RATING = 5;

  private final LovService lovs;

  /**
   * Creates the checks.
   *
   * @param lovs benefit lines and factors
   */
  public EbProposalRules(LovService lovs) {
    this.lovs = lovs;
  }

  /**
   * Checks the input.
   *
   * @param input proposal
   * @param torItemIds items of the TOR released on the cycle
   * @param today business date
   */
  void check(ProposalInput input, Set<Long> torItemIds, LocalDate today) {
    if (input.document() == null) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_DOCUMENT_REQUIRED", "Attach the insurer's proposal document");
    }
    if (input.receivedOn() != null && input.receivedOn().isAfter(today)) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_DATE_FUTURE", "The date received cannot be after today");
    }
    if (input.lines().isEmpty()) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_LINES_REQUIRED", "Enter the premium of at least one plan");
    }
    int row = 1;
    for (EbProposalLine.Data line : input.lines()) {
      checkLine(line, row++, today);
    }
    checkItems(input.items(), torItemIds);
    checkFactors(input.factors(), today);
  }

  private static void checkItems(List<EbProposalItem.Data> items, Set<Long> torItemIds) {
    for (EbProposalItem.Data item : items) {
      if (!torItemIds.contains(item.torItemId())) {
        throw new BusinessRuleException(
            "EB_PROPOSAL_ITEM_UNKNOWN", "Answer the items of the TOR released on this cycle");
      }
      if (item.offeredValue() == null || item.offeredValue().isBlank()) {
        throw new BusinessRuleException(
            "EB_PROPOSAL_ITEM_EMPTY", "Enter what the insurer offers for each answered item");
      }
    }
  }

  private void checkFactors(List<EbProposalFactor.Data> factors, LocalDate today) {
    Set<String> seen = new HashSet<>();
    for (EbProposalFactor.Data factor : factors) {
      lovs.requireValid(EbCodes.LOV_CAPABILITY_FACTOR, factor.factorCode(), today);
      if (!seen.add(factor.factorCode())) {
        throw new BusinessRuleException(
            "EB_PROPOSAL_FACTOR_TWICE", "Rate each capability factor once");
      }
      if (factor.rating() != null && (factor.rating() < 1 || factor.rating() > MAX_RATING)) {
        throw new BusinessRuleException(
            "EB_PROPOSAL_FACTOR_RATING", "Rate the capability factors from 1 to 5");
      }
    }
  }

  private void checkLine(EbProposalLine.Data line, int row, LocalDate today) {
    if (line.benefitLine() == null || line.planCode() == null || line.planCode().isBlank()) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_LINE_PLAN", "Plan " + row + ": select the benefit line and enter the plan");
    }
    lovs.requireValid(EbCodes.LOV_BENEFIT_LINE, line.benefitLine(), today);
    checkAmounts(line, row);
  }

  private static void checkAmounts(EbProposalLine.Data line, int row) {
    if (line.annualPremium() == null || line.annualPremium().signum() < 0) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_PREMIUM", "Plan " + row + ": enter an annual premium of zero or more");
    }
    boolean negativeMembers = line.members() != null && line.members() < 0;
    if (negative(line.sumInsured()) || negative(line.premiumRate()) || negativeMembers) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_AMOUNTS", "Plan " + row + ": amounts and members cannot be negative");
    }
  }

  private static boolean negative(BigDecimal value) {
    return value != null && value.signum() < 0;
  }

  /**
   * Refuses a revised proposal that skips a requested change on a TOR item.
   *
   * @param revision the revision answered
   * @param items answers of the proposal
   */
  static void requireRevisionAnswered(EbRevisionRequest revision, List<EbProposalItem.Data> items) {
    Set<Long> answered = new HashSet<>();
    items.forEach(i -> answered.add(i.torItemId()));
    revision.getItems().stream()
        .filter(i -> i.getTorItemId() != null && !answered.contains(i.getTorItemId()))
        .findFirst()
        .ifPresent(
            i -> {
              throw new BusinessRuleException(
                  "EB_REVISION_ITEM_UNANSWERED",
                  "Answer the requested change "
                      + i.getSortOrder()
                      + ": "
                      + i.getRequestedChange());
            });
  }
}
