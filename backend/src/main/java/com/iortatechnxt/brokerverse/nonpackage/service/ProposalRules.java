package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails.Item;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails.Section;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Validates a PRF draft (BRNB.005): usable client, usable product offered to the segment, valid
 * period and codes, active panel insurers; the items keep only the block of the product line's item
 * kind; and the TSU routing decision of the catalog rules (BRNB.098).
 */
@Component
public class ProposalRules {

  private final ClientService clients;
  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final TsuRoutingService tsu;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param clients clients
   * @param catalog products
   * @param insurers insurer panel
   * @param tsu TSU routing rules
   * @param lovs lists of values
   * @param clock clock
   */
  public ProposalRules(
      ClientService clients,
      ProductCatalogService catalog,
      InsurerService insurers,
      TsuRoutingService tsu,
      LovService lovs,
      Clock clock) {
    this.clients = clients;
    this.catalog = catalog;
    this.insurers = insurers;
    this.tsu = tsu;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Resolves a draft.
   *
   * @param companyId company
   * @param draft draft
   * @return client, product and cleaned risk details
   */
  public Checked check(Long companyId, ProposalDraft draft) {
    requireHeader(draft);
    Client client = clients.requireUsable(draft.clientId());
    RiskProduct product = catalog.requireUsableProduct(draft.productCode().strip());
    if (!product.allowsSegment(blank(draft.marketSegment()))) {
      throw new BusinessRuleException(
          "SEGMENT_NOT_ALLOWED",
          "Product " + product.getCode() + " is not offered to segment " + draft.marketSegment());
    }
    requirePeriod(draft);
    LocalDate today = LocalDate.now(clock);
    lovs.validateOptional("MARKET_SEGMENT", blank(draft.marketSegment()), today);
    lovs.validateOptional("SOURCE_CHANNEL", blank(draft.sourceChannel()), today);
    draft.insurers().forEach(code -> insurers.requireUsableInsurer(companyId, code));
    RiskItemKind kind = catalog.requireLine(product.getLineCode()).getRiskItemKind();
    return new Checked(client, product, clean(kind, draft.details()));
  }

  private static void requireHeader(ProposalDraft draft) {
    Map<String, String> missing = new LinkedHashMap<>();
    if (draft.clientId() == null) {
      missing.put("clientId", "Select the client or prospect");
    }
    if (blank(draft.productCode()) == null) {
      missing.put("productCode", "Select the product line and risk code");
    }
    if (!missing.isEmpty()) {
      throw new FieldValidationException("PRF_INCOMPLETE", "Complete the PRF header", missing);
    }
  }

  private static void requirePeriod(ProposalDraft draft) {
    if (draft.periodFrom() != null
        && draft.periodTo() != null
        && !draft.periodTo().isAfter(draft.periodFrom())) {
      throw new BusinessRuleException(
          "PRF_PERIOD_INVALID", "The period end must be after the period start");
    }
  }

  private static RiskDetails clean(RiskItemKind kind, RiskDetails given) {
    return new RiskDetails(
        given.sections().stream()
            .filter(s -> blank(s.heading()) != null || blank(s.text()) != null)
            .map(s -> new Section(blank(s.heading()), blank(s.text())))
            .toList(),
        given.items().stream()
            .map(i -> new Item(Math.max(1, i.riskGroup()), keep(kind, i.data())))
            .toList());
  }

  /**
   * TSU routing of a PRF: a non-package risk always goes to TSU (rule NON_PACKAGE); a package risk
   * only when a rule or its package limit applies.
   *
   * @param product product
   * @param details risk details
   * @return decision
   */
  public TsuDecision route(RiskProduct product, RiskDetails details) {
    List<RiskItemData> data = details.items().stream().map(Item::data).toList();
    return tsu.evaluate(
        product,
        new TsuFacts(
            product.isPackaged(),
            product.getLineCode(),
            (int) data.stream().filter(d -> d.vehicle() != null).count(),
            (int) data.stream().filter(d -> d.location() != null).count(),
            sumInsured(details),
            null));
  }

  /**
   * Total sum insured of the items (a location counts the items insured there when given).
   *
   * @param details risk details
   * @return total
   */
  public static BigDecimal sumInsured(RiskDetails details) {
    return details.items().stream()
        .map(i -> itemSum(i.data()))
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal itemSum(RiskItemData d) {
    if (d.location() == null || d.location().insuredItems().isEmpty()) {
      return d.sumInsured();
    }
    return d.location().insuredItems().stream()
        .map(InsuredItem::sumInsured)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static RiskItemData keep(RiskItemKind kind, RiskItemData d) {
    if (d == null) {
      return RiskItemData.generic(null, null, null);
    }
    return switch (kind) {
      case VEHICLE ->
          new RiskItemData(
              d.description(),
              d.sumInsured(),
              d.rate(),
              d.biLimit(),
              d.pdLimit(),
              d.vehicle(),
              null,
              null);
      case PROPERTY_LOCATION ->
          new RiskItemData(
              d.description(), d.sumInsured(), d.rate(), null, null, null, d.location(), null);
      case PERSON ->
          new RiskItemData(
              d.description(), d.sumInsured(), d.rate(), null, null, null, null, d.person());
      case GENERIC -> RiskItemData.generic(d.description(), d.sumInsured(), d.rate());
    };
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A checked draft.
   *
   * @param client client
   * @param product product
   * @param details cleaned risk details
   */
  public record Checked(Client client, RiskProduct product, RiskDetails details) {}
}
