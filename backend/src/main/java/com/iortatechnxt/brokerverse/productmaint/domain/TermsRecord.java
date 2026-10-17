package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.Set;

/**
 * The record a comparative table belongs to: a quotation request of a non-package product
 * (ProposalRequest) or a package request (PackageRequest).
 *
 * @param type record type
 * @param id record id
 */
public record TermsRecord(String type, Long id) {

  /** A quotation request of a non-package product. */
  public static final String QUOTATION = "ProposalRequest";

  /** A package request. */
  public static final String PACKAGE = "PackageRequest";

  private static final Set<String> TYPES = Set.of(QUOTATION, PACKAGE);

  /** Checks the type. */
  public TermsRecord {
    if (!TYPES.contains(type)) {
      throw new BusinessRuleException(
          "PM_TERMS_RECORD", "A comparative table belongs to a quotation or package request");
    }
  }

  /**
   * Whether the record is a quotation request of a non-package product.
   *
   * @return true for a quotation request
   */
  public boolean quotation() {
    return QUOTATION.equals(type);
  }
}
