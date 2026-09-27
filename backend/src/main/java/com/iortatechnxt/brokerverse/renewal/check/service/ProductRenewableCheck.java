package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import org.springframework.stereotype.Component;

/**
 * {@code PRODUCT_RENEWABLE} (PQ11): the product of the renewal (or the BIBS package a migrated
 * policy resolved to) may be sold for a renewal on the business date ({@code
 * ProductCatalogService.requireSellable} with the RENEWAL purpose). An expired package version is
 * RQ30.
 */
@Component
public class ProductRenewableCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "PRODUCT_RENEWABLE";

  private final ProductCatalogService catalog;

  /**
   * Creates the check.
   *
   * @param catalog product catalogue
   */
  public ProductRenewableCheck(ProductCatalogService catalog) {
    this.catalog = catalog;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    String product =
        c.getResolvedProductCode() != null
            ? c.getResolvedProductCode()
            : c.getSnapshot().product() == null ? null : c.getSnapshot().product().productCode();
    if (product == null) {
      return Verdict.notApplicable("No BIBS product yet");
    }
    try {
      catalog.requireSellable(product, RatingQuery.Purpose.RENEWAL, context.today());
      return Verdict.pass("Product " + product + " can be renewed");
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return Verdict.fail(e.getMessage(), product);
    }
  }
}
