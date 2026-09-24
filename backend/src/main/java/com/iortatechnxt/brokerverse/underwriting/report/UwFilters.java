package com.iortatechnxt.brokerverse.underwriting.report;

import com.iortatechnxt.brokerverse.underwriting.domain.Quotation;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;

/**
 * Optional range filters of the underwriting reports; null = all values.
 *
 * @param branchId branch
 * @param businessLine class (line of business)
 * @param productCode product code
 * @param customerCode client code
 * @param brokerCode intermediary code
 */
public record UwFilters(
    Long branchId,
    String businessLine,
    String productCode,
    String customerCode,
    String brokerCode) {

  /**
   * Tests a policy.
   *
   * @param p policy
   * @return true when it passes every filter
   */
  public boolean test(PolicySnapshot p) {
    return test(
        p.branchId(), p.businessLine(), p.productCode(), p.customerCode(), p.intermediaryCode());
  }

  /**
   * Tests a quotation.
   *
   * @param q quotation (product and parties loaded)
   * @return true when it passes every filter
   */
  public boolean test(Quotation q) {
    return test(
        q.getBranchId(),
        q.getProduct().getBusinessLine(),
        q.getProduct().getCode(),
        q.getCustomer().getCode(),
        q.getIntermediary() == null ? null : q.getIntermediary().getCode());
  }

  /**
   * Tests raw values.
   *
   * @param branch branch
   * @param lob class
   * @param product product code
   * @param customer client code
   * @param broker intermediary code
   * @return true when every filter matches
   */
  public boolean test(Long branch, String lob, String product, String customer, String broker) {
    boolean branchMatches = branchId == null || branchId.equals(branch);
    return branchMatches
        && matches(businessLine, lob)
        && matches(productCode, product)
        && matches(customerCode, customer)
        && matches(brokerCode, broker);
  }

  private static boolean matches(String filter, String value) {
    return filter == null || filter.equals(value);
  }
}
