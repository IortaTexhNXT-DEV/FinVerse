package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest.ClientRef;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest.RequestForm;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Validation of the Package Request Form (BRPM.008/011, PMADD01): the header against the masters
 * (line, cover type, product, client, reason, segments, insurers) when a draft is saved, and the
 * completeness check before submission ({@code PKG_REQUEST_INCOMPLETE}, BRPM.021).
 */
@Component
public class PackageRequestRules {

  /** Error code of an incomplete request. */
  public static final String INCOMPLETE = "PKG_REQUEST_INCOMPLETE";

  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final ClientService clients;
  private final LovService lovs;
  private final ProductVersionQueryService versions;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param catalog product lines, cover types and products
   * @param insurers insurer panel
   * @param clients CRM clients
   * @param lovs lists of values
   * @param versions catalog package versions
   * @param clock clock
   */
  public PackageRequestRules(
      ProductCatalogService catalog,
      InsurerService insurers,
      ClientService clients,
      LovService lovs,
      ProductVersionQueryService versions,
      Clock clock) {
    this.catalog = catalog;
    this.insurers = insurers;
    this.clients = clients;
    this.lovs = lovs;
    this.versions = versions;
    this.clock = clock;
  }

  /**
   * Checks a draft against the masters and builds the form header.
   *
   * @param companyId company
   * @param draft form data
   * @return header and cleaned terms
   */
  public Checked check(Long companyId, RequestDraft draft) {
    LocalDate today = LocalDate.now(clock);
    if (draft.title() == null || draft.title().isBlank()) {
      throw new BusinessRuleException(INCOMPLETE, "Enter the package or programme name");
    }
    catalog.requireLine(draft.lineCode());
    checkCoverType(draft);
    ProductFacts product = product(draft);
    lovs.requireValid("PKG_REQUEST_REASON", draft.reason(), today);
    draft.marketSegments().forEach(s -> lovs.requireValid("MARKET_SEGMENT", s, today));
    ClientRef client = client(draft);
    PackageTerms terms = TermsChecks.clean(draft.terms());
    terms.insurerCodes().forEach(code -> insurers.requireUsableInsurer(companyId, code));
    TermsChecks.checkNumbers(terms);
    RequestForm form =
        new RequestForm(
            draft.scope(),
            draft.title().strip(),
            client,
            draft.lineCode(),
            blank(draft.coverTypeCode()),
            product.code(),
            product.baseVersionNo(),
            draft.marketSegments(),
            draft.reason(),
            blank(draft.reasonNote()),
            negotiation(draft));
    return new Checked(form, terms);
  }

  private void checkCoverType(RequestDraft draft) {
    String cover = blank(draft.coverTypeCode());
    if (cover != null
        && catalog.coverTypes().stream()
            .noneMatch(
                t -> t.getLineCode().equals(draft.lineCode()) && t.getCode().equals(cover))) {
      throw new BusinessRuleException(
          "PKG_COVER_TYPE_INVALID", cover + " is not a cover type of " + draft.lineCode());
    }
  }

  private ProductFacts product(RequestDraft draft) {
    String code = blank(draft.productCode());
    if (!draft.type().needsProduct()) {
      return new ProductFacts(null, null);
    }
    if (code == null) {
      throw new BusinessRuleException(
          INCOMPLETE, "Select the package product of the " + draft.type() + " request");
    }
    RiskProduct product = catalog.requireProduct(code);
    if (!product.isPackaged()) {
      throw new BusinessRuleException(
          "PKG_PRODUCT_NOT_PACKAGED", code + " is not a packaged product");
    }
    if (!product.getLineCode().equals(draft.lineCode())) {
      throw new BusinessRuleException(
          "PKG_LINE_MISMATCH", code + " belongs to line " + product.getLineCode());
    }
    Integer base = draft.baseVersionNo();
    if (base == null) {
      base = latestVersion(code);
    }
    return new ProductFacts(code, base);
  }

  private Integer latestVersion(String code) {
    return versions
        .current(code)
        .or(() -> versions.versions(code).stream().findFirst())
        .map(ProductVersionView::versionNo)
        .orElse(null);
  }

  private ClientRef client(RequestDraft draft) {
    if (draft.scope() == RequestScope.GENERIC) {
      return null;
    }
    if (draft.clientId() == null) {
      throw new BusinessRuleException(
          INCOMPLETE, "Select the client of the client-specific package (PQ14)");
    }
    Client c = clients.requireUsable(draft.clientId());
    return new ClientRef(c.getId(), c.getCode(), c.getDisplayName());
  }

  private static boolean negotiation(RequestDraft draft) {
    return switch (draft.type()) {
      case NEW, AMEND -> true;
      case RETIRE -> false;
      default ->
          draft.negotiationRequired() == null
              ? draft.type().negotiatesByDefault()
              : draft.negotiationRequired();
    };
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A checked draft.
   *
   * @param form form header
   * @param terms cleaned requested terms
   */
  public record Checked(RequestForm form, PackageTerms terms) {}

  private record ProductFacts(String code, Integer baseVersionNo) {}
}
