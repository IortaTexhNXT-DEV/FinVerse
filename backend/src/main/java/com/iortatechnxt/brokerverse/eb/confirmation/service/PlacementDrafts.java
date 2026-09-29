package com.iortatechnxt.brokerverse.eb.confirmation.service;

import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService.ProductFilter;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmation;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.placement.service.LinePlacement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The account draft of each confirmed line (FR-EB-046 R1): the EB product of the line (the line's
 * product, else the first sellable product of the benefit line's product line), the chosen insurer,
 * the client's market segment, one policy year from the day after the current expiry (renewal) or
 * the target inception, the proposal's currency and one generic risk item whose sum insured and
 * rate give the chosen annual premium (the TSI of GLI / GPA, else the premium itself at 100%).
 */
@Component
@Transactional(readOnly = true)
public class PlacementDrafts {

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final int RATE_SCALE = 8;
  private static final String DEFAULT_SEGMENT = "CORBANK";
  private static final String SOURCE_CHANNEL = "EMAIL";

  private final ProductCatalogService catalog;
  private final ClientService clients;
  private final Clock clock;

  /**
   * Creates the builder.
   *
   * @param catalog EB products
   * @param clients client segment
   * @param clock clock
   */
  public PlacementDrafts(ProductCatalogService catalog, ClientService clients, Clock clock) {
    this.catalog = catalog;
    this.clients = clients;
    this.clock = clock;
  }

  /**
   * The placement of a confirmed line.
   *
   * @param programme programme
   * @param cycle cycle
   * @param choice confirmed line
   * @param proposal chosen proposal
   * @return line placement
   */
  public LinePlacement placement(
      EbProgramme programme, EbCycle cycle, EbClientConfirmation.Line choice, EbProposal proposal) {
    EbProgrammeLine line = programme.line(choice.getLineNo());
    LocalDate from = inception(cycle, line);
    String segment = clients.get(programme.getClientId()).getMarketSegment();
    AccountDraft draft =
        new AccountDraft(
            programme.getClientId(),
            product(line),
            segment == null || segment.isBlank() ? DEFAULT_SEGMENT : segment,
            SOURCE_CHANNEL,
            choice.getInsurerCode(),
            null,
            from,
            from.plusYears(1),
            false,
            1,
            proposal.getCurrency(),
            PaymentArrangement.VIA_BROKER,
            Mortgage.NONE,
            null,
            List.of(item(choice, proposal)),
            null,
            null,
            null);
    return new LinePlacement(choice.getLineNo(), draft, null);
  }

  private LocalDate inception(EbCycle cycle, EbProgrammeLine line) {
    if (cycle.getBusinessType() == BusinessType.RENEWAL && line.getPeriodTo() != null) {
      return line.getPeriodTo().plusDays(1);
    }
    return cycle.getTargetInception() != null
        ? cycle.getTargetInception()
        : BusinessClock.today(clock);
  }

  private String product(EbProgrammeLine line) {
    if (line.getProductCode() != null) {
      return line.getProductCode();
    }
    return catalog
        .products(new ProductFilter(line.getBenefitLine(), null, null, null, true))
        .stream()
        .filter(RiskProduct::isSellable)
        .map(RiskProduct::getCode)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_PRODUCT_MISSING",
                    "No Employee Benefits product is set up for " + line.getBenefitLine()));
  }

  /**
   * The risk item that gives the chosen annual premium.
   *
   * @param choice confirmed line
   * @param proposal chosen proposal
   * @return generic item
   */
  static RiskItemData item(EbClientConfirmation.Line choice, EbProposal proposal) {
    BigDecimal premium = choice.getAnnualPremium();
    BigDecimal tsi = choice.getSumInsured();
    String description =
        choice.getBenefitLine()
            + " - "
            + proposal.getProposalNo()
            + " ("
            + plans(proposal, choice)
            + ")";
    if (tsi == null || tsi.signum() == 0) {
      return RiskItemData.generic(description, premium, HUNDRED);
    }
    return RiskItemData.generic(
        description, tsi, premium.multiply(HUNDRED).divide(tsi, RATE_SCALE, RoundingMode.HALF_UP));
  }

  private static String plans(EbProposal proposal, EbClientConfirmation.Line choice) {
    return String.join(
        ", ",
        proposal.getLines().stream()
            .filter(l -> l.getBenefitLine().equals(choice.getBenefitLine()))
            .map(l -> l.getPlanCode())
            .toList());
  }
}
