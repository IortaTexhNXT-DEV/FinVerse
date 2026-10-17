package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.service.AuditModules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The record types of the Product Maintenance audit logs (BDOI FRS FRPM.021.01 and FRPM.021.02):
 * the Product Maintenance records, and the workflow steps of one record when its reference is given
 * (the history of a package or quotation request).
 */
public final class PmAuditScope {

  /** Record type of the workflow steps. */
  public static final String WORK_CASE = "WorkCase";

  private PmAuditScope() {}

  /**
   * The record types to search.
   *
   * @param requested record types asked for (comma separated), blank for all
   * @param reference reference number, blank for all records
   * @return record types, never empty
   */
  public static List<String> types(String requested, String reference) {
    List<String> allowed = new ArrayList<>(AuditModules.PRODUCT_MAINTENANCE_TYPES);
    if (reference != null && !reference.isBlank()) {
      allowed.add(WORK_CASE);
    }
    if (requested == null || requested.isBlank()) {
      return allowed;
    }
    List<String> asked =
        Arrays.stream(requested.split(",")).map(String::strip).filter(allowed::contains).toList();
    return asked.isEmpty() ? allowed : asked;
  }
}
