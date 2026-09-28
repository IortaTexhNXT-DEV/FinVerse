package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Upload of the handling-fee billing list (BRIDSP-31; SP SQ13): one billed record per row with the
 * PN (CLPC payments) or the location reference (OTC payments), the amount and the billing date,
 * optionally linked to a masterlist record by its number.
 */
@Component
public class HandlingFeeBillingHandler implements BulkImportHandler {

  private static final String AMOUNT = "Amount";

  /** Handler code. */
  public static final String CODE = "SBM_HANDLING_FEE_BILLING";

  private static final String SBM_NO = "Masterlist No";
  private static final String PN = "PN No";
  private static final String LOCATION = "Location Reference";

  private final HandlingFeeService fees;
  private final SbmPolicyRepository policies;

  /**
   * Creates the handler.
   *
   * @param fees handling fees
   * @param policies masterlist
   */
  public HandlingFeeBillingHandler(HandlingFeeService fees, SbmPolicyRepository policies) {
    this.fees = fees;
    this.policies = policies;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Handling-fee billing list";
  }

  @Override
  public String permission() {
    return "SBM_HANDLING_FEE";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(SBM_NO, "Masterlist number of the policy, if known", "SBM-2026-000012"),
        BulkColumn.optional(PN, "PN number carried by CLPC payments", "PN-2026-000123"),
        BulkColumn.optional(LOCATION, "Location reference carried by OTC payments", "LOC-000451"),
        new BulkColumn(AMOUNT, "Handling fee billed", true, Type.NUMBER, "1500.00"),
        new BulkColumn("Billing Date", "Billing date", true, Type.DATE, "2026-09-01"));
  }

  @Override
  public String sanitize(String header, String value) {
    String v = value.trim().replaceAll("\\s+", " ");
    return PN.equals(header) ? BulkImportHandler.identifier(v) : v;
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text(PN) == null && row.text(LOCATION) == null) {
      errors.add("Enter the PN number or the location reference");
    }
    if (row.number(AMOUNT) != null && row.number(AMOUNT).signum() <= 0) {
      errors.add("The amount must be above zero");
    }
    String sbmNo = row.text(SBM_NO);
    if (sbmNo != null
        && policies
            .findBySbmNo(sbmNo)
            .filter(p -> p.getCompanyId().equals(context.companyId()))
            .isEmpty()) {
      errors.add("Masterlist number " + sbmNo + " is unknown");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Long policyId =
        row.text(SBM_NO) == null
            ? null
            : policies.findBySbmNo(row.text(SBM_NO)).map(SbmPolicy::getId).orElse(null);
    return fees.bill(
            context.companyId(),
            new SbmHandlingFee.Bill(
                policyId,
                row.text(PN),
                row.text(LOCATION),
                row.number(AMOUNT),
                null,
                row.date("Billing Date")),
            context.jobNo())
        .getFeeNo();
  }
}
