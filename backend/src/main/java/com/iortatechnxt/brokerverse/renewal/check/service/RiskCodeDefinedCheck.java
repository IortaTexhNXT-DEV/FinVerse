package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import org.springframework.stereotype.Component;

/**
 * {@code RISK_CODE_DEFINED} (Annex BRRN.020 SC-09; FR-RN-020): a risk code that is not defined in
 * the criteria, that is not a product of the catalogue (for example a legacy code a migrated policy
 * carries), goes to manual review. Renewals whose package was resolved by the package code map are
 * checked on the resolved product.
 */
@Component
public class RiskCodeDefinedCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "RISK_CODE_DEFINED";

  private final ProductCatalogService catalog;

  /**
   * Creates the check.
   *
   * @param catalog product catalogue
   */
  public RiskCodeDefinedCheck(ProductCatalogService catalog) {
    this.catalog = catalog;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    CandidateSnapshot s = context.candidate().getSnapshot();
    String risk =
        context.candidate().getResolvedProductCode() != null
            ? context.candidate().getResolvedProductCode()
            : s.product() == null ? null : s.product().productCode();
    if (risk == null) {
      return Verdict.fail("The risk code is missing: manual review", null);
    }
    try {
      catalog.requireProduct(risk);
      return Verdict.pass("Risk code " + risk + " is defined");
    } catch (ResourceNotFoundException e) {
      return Verdict.fail("Risk code " + risk + " is not defined: manual review", risk);
    }
  }
}
