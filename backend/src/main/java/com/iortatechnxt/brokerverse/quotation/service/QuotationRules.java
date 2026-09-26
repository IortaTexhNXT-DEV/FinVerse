package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.Terms;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Validates and resolves a quotation draft (BRNB.043/063, MKTID.011): usable client (a prospect is
 * enough), usable product offered to the segment, direct payment only where the product allows it,
 * valid period, validity and codes, active insurer branch; items keep only the block of the product
 * line's item kind.
 */
@Component
public class QuotationRules {

  /** Default validity of a quotation in days. */
  public static final String VALIDITY_DAYS = "QUOTATION_VALIDITY_DAYS";

  private static final int DEFAULT_VALIDITY = 30;
  private static final String PRODUCT = "Product ";

  private final ClientService clients;
  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final LovService lovs;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param clients clients
   * @param catalog products
   * @param insurers insurers
   * @param lovs lists of values
   * @param parameters business parameters
   * @param clock clock
   */
  public QuotationRules(
      ClientService clients,
      ProductCatalogService catalog,
      InsurerService insurers,
      LovService lovs,
      SystemParameterService parameters,
      Clock clock) {
    this.clients = clients;
    this.catalog = catalog;
    this.insurers = insurers;
    this.lovs = lovs;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Resolves a draft.
   *
   * @param companyId company
   * @param draft draft
   * @return client, product and content (not yet priced)
   */
  public Resolved resolve(Long companyId, QuotationDraft draft) {
    requireHeader(draft);
    Client client = clients.requireUsable(draft.clientId());
    Offer offer = offer(companyId, draft);
    return new Resolved(client, offer.product(), offer.content());
  }

  /**
   * Checks everything but the client (bulk rows whose prospect does not exist yet).
   *
   * @param companyId company
   * @param draft draft (client ignored)
   * @return product and content, not yet priced
   */
  public Offer offer(Long companyId, QuotationDraft draft) {
    if (blankToNull(draft.productCode()) == null) {
      throw new FieldValidationException(
          "QUOTATION_INCOMPLETE",
          "Select the product",
          Map.of("productCode", "Select the product"));
    }
    RiskProduct product =
        catalog.requireSellable(
            draft.productCode(), RatingQuery.Purpose.NEW_BUSINESS, LocalDate.now(clock));
    String segment = blankToNull(draft.marketSegment());
    if (!product.allowsSegment(segment)) {
      throw new BusinessRuleException(
          "SEGMENT_NOT_ALLOWED",
          PRODUCT + product.getCode() + " is not offered to segment " + segment);
    }
    Terms terms = draft.terms() == null ? emptyTerms() : draft.terms();
    checkTerms(companyId, product, terms);
    checkCodes(draft);
    RiskItemKind kind = catalog.requireLine(product.getLineCode()).getRiskItemKind();
    QuotationContent content =
        new QuotationContent(
            blankToNull(terms.insurerCode()),
            blankToNull(terms.insurerCode()) == null ? null : blankToNull(terms.insurerBranch()),
            terms.periodFrom(),
            terms.periodTo(),
            terms.validUntil() != null ? terms.validUntil() : defaultValidity(),
            terms.directPayment(),
            basis(terms.ratingBasis()),
            blankToNull(terms.remarks()),
            draft.items().stream()
                .map(i -> new QuotationItem(i.group(), forKind(kind, i.data()), null, null))
                .toList(),
            null);
    return new Offer(product, content);
  }

  private static void requireHeader(QuotationDraft draft) {
    Map<String, String> errors = new LinkedHashMap<>();
    if (draft.clientId() == null) {
      errors.put("clientId", "Select the client or prospect");
    }
    if (blankToNull(draft.productCode()) == null) {
      errors.put("productCode", "Select the product");
    }
    if (!errors.isEmpty()) {
      throw new FieldValidationException(
          "QUOTATION_INCOMPLETE", "Select the client and the product", errors);
    }
  }

  private void checkTerms(Long companyId, RiskProduct product, Terms terms) {
    if (terms.directPayment() && !product.isDirectPaymentEligible()) {
      throw new BusinessRuleException(
          "DIRECT_PAYMENT_NOT_ELIGIBLE",
          PRODUCT + product.getCode() + " cannot be paid directly to the insurer");
    }
    if (terms.periodFrom() != null
        && terms.periodTo() != null
        && !terms.periodTo().isAfter(terms.periodFrom())) {
      throw new BusinessRuleException(
          "QUOTATION_PERIOD_INVALID", "The period end must be after the period start");
    }
    checkInsurer(companyId, terms);
  }

  private void checkInsurer(Long companyId, Terms terms) {
    String insurer = blankToNull(terms.insurerCode());
    if (insurer != null) {
      insurers.requireUsableInsurer(companyId, insurer);
      if (blankToNull(terms.insurerBranch()) != null) {
        insurers.requireUsableBranch(companyId, insurer, terms.insurerBranch());
      }
    }
  }

  private void checkCodes(QuotationDraft draft) {
    LocalDate today = LocalDate.now(clock);
    lovs.validateOptional("MARKET_SEGMENT", blankToNull(draft.marketSegment()), today);
    lovs.validateOptional("SOURCE_CHANNEL", blankToNull(draft.sourceChannel()), today);
    for (QuotationDraft.DraftItem item : draft.items()) {
      RiskItemData data = item.data();
      if (data != null && data.vehicle() != null) {
        lovs.validateOptional("VEHICLE_BODY_TYPE", data.vehicle().bodyType(), today);
      }
      if (data != null && data.location() != null) {
        lovs.validateOptional("OCCUPANCY", data.location().occupancy(), today);
        lovs.validateOptional("CONSTRUCTION_CLASS", data.location().constructionClass(), today);
      }
    }
  }

  /**
   * Validity when none is given: today plus the configured number of days.
   *
   * @return date
   */
  public LocalDate defaultValidity() {
    return LocalDate.now(clock).plusDays(parameters.intValue(VALIDITY_DAYS, DEFAULT_VALIDITY));
  }

  private static String basis(String given) {
    String value = blankToNull(given);
    if (value == null) {
      return PeriodBasis.ANNUAL.name();
    }
    if (Arrays.stream(PeriodBasis.values()).noneMatch(b -> b.name().equals(value))) {
      throw new BusinessRuleException("RATING_BASIS_INVALID", "Unknown rating basis " + value);
    }
    return value;
  }

  private static RiskItemData forKind(RiskItemKind kind, RiskItemData i) {
    RiskItemData data = i == null ? RiskItemData.generic(null, null, null) : i;
    return new RiskItemData(
        blankToNull(data.description()),
        data.sumInsured(),
        data.rate(),
        data.biLimit(),
        data.pdLimit(),
        kind == RiskItemKind.VEHICLE ? data.vehicle() : null,
        kind == RiskItemKind.PROPERTY_LOCATION ? data.location() : null,
        kind == RiskItemKind.PERSON ? data.person() : null);
  }

  private static Terms emptyTerms() {
    return new Terms(null, null, null, null, null, false, null, null);
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A resolved draft.
   *
   * @param client client
   * @param product product
   * @param content content, not yet priced
   */
  public record Resolved(Client client, RiskProduct product, QuotationContent content) {}

  /**
   * The product and content of a draft.
   *
   * @param product product
   * @param content content, not yet priced
   */
  public record Offer(RiskProduct product, QuotationContent content) {}
}
