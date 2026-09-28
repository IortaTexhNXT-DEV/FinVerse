package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyParties;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyPolicy;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_LEGACY_POLICIES} (RQ27; RENEWAL_DESIGN section 10): the fallback of the
 * migrated policy headers while Data Migration's {@code LegacyPolicySource} is not connected. Each
 * row becomes a candidate of source LEGACY, keyed by the legacy reference; it renews on the New
 * Business path, or as is once its legacy package resolves to a BIBS package version.
 */
@Component
public class LegacyPoliciesBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_LEGACY_POLICIES";

  private static final String LEGACY_REF = "Legacy Policy Reference";
  private static final String SYSTEM = "Source System";
  private static final String POLICY_NO = "Policy No";
  private static final String CLIENT_CODE = "Client Code";
  private static final String CLIENT_NAME = "Client Name";
  private static final String PRODUCT = "Risk Code";
  private static final String LINE = "Product Line";
  private static final String PACKAGE = "Legacy Package";
  private static final String PACKAGE_VERSION = "Legacy Package Version";
  private static final String INSURER = "Insurer";
  private static final String INCEPTION = "Inception Date";
  private static final String EXPIRY = "Expiry Date";
  private static final String SUM_INSURED = "Sum Insured";
  private static final String PREMIUM = "Gross Premium";
  private static final String PN = "PN Numbers";
  private static final String OFFICER = "Account Officer";
  private static final String UNIT = "Sales Unit";
  private static final String SEGMENT = "Market Segment";
  private static final String MORTGAGEE = "Mortgagee Bank";
  private static final String URGENT = "Urgent";

  private final ExtractionService extraction;
  private final RenewalCandidateRepository candidates;

  /**
   * Creates the handler.
   *
   * @param extraction candidate creation
   * @param candidates candidates (duplicate guard)
   */
  public LegacyPoliciesBulkHandler(
      ExtractionService extraction, RenewalCandidateRepository candidates) {
    this.extraction = extraction;
    this.candidates = candidates;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - expiring policies of the legacy systems";
  }

  @Override
  public String permission() {
    return Permission.RNW_EXTRACT.name();
  }

  @Override
  public String filledBy() {
    return "The renewal team, from the expiring-policy extracts of the legacy systems";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Setup, tab Go-live, button Upload Migrated Policies";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(LEGACY_REF, "Policy reference in the legacy system", "QPS-FI-0012345"),
        BulkColumn.required(SYSTEM, "Legacy system of the policy", "QPS").values("EBIX", "QPS"),
        BulkColumn.required(POLICY_NO, "Policy number", "FI-2027-0012345"),
        BulkColumn.required(CLIENT_CODE, "Client code in BIBS", "CL-2026-000001").master("client"),
        BulkColumn.required(CLIENT_NAME, "Client name", "Juan Dela Cruz"),
        BulkColumn.optional(PRODUCT, "Risk code in BIBS when known", "PAR01")
            .master("product (risk code)"),
        BulkColumn.optional(LINE, "Product line", "PROPERTY"),
        BulkColumn.optional(PACKAGE, "Package code in the legacy system", "QPS-HOME-A"),
        BulkColumn.optional(PACKAGE_VERSION, "Package version in the legacy system", "3"),
        BulkColumn.required(INSURER, "Insurer of the policy", "INS-MGIC").master("insurer"),
        new BulkColumn(INCEPTION, "Start of the term", false, BulkColumn.Type.DATE, "2027-03-01"),
        new BulkColumn(EXPIRY, "End of the term", true, BulkColumn.Type.DATE, "2028-03-01"),
        new BulkColumn(
            SUM_INSURED, "Total sum insured", false, BulkColumn.Type.NUMBER, "3500000.00"),
        new BulkColumn(PREMIUM, "Gross premium", false, BulkColumn.Type.NUMBER, "12500.00"),
        BulkColumn.optional(PN, "Promissory note numbers of the loan", "PN-778812")
            .format("Text; several numbers separated by commas"),
        BulkColumn.optional(OFFICER, "User ID of the account officer", "jdelacruz")
            .allowed("User ID of an active user"),
        BulkColumn.optional(UNIT, "Sales team", "T-CBG1").master("sales team"),
        BulkColumn.optional(SEGMENT, "Market segment", "CBG").lov("MARKET_SEGMENT"),
        BulkColumn.optional(MORTGAGEE, "Mortgagee bank when mortgaged", "BDO Unibank"),
        new BulkColumn(URGENT, "Y to flag the renewal urgent", false, BulkColumn.Type.YES_NO, "N"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LEGACY_REF);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (candidates
        .findByCompanyIdAndSourceAndSourceRef(
            context.companyId(), CandidateSource.LEGACY, row.text(LEGACY_REF))
        .isPresent()) {
      errors.add("Legacy policy " + row.text(LEGACY_REF) + " is already a renewal");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return extraction
        .createLegacy(context.companyId(), header(row), null, row.yes(URGENT))
        .getRenewalRef();
  }

  private static LegacyHeader header(BulkRow row) {
    String mortgagee = row.text(MORTGAGEE);
    return new LegacyHeader(
        row.text(LEGACY_REF),
        row.text(SYSTEM),
        null,
        new LegacyPolicy(
            row.text(POLICY_NO),
            null,
            row.text(PRODUCT),
            row.text(LINE),
            row.text(PACKAGE),
            row.text(PACKAGE_VERSION),
            row.date(INCEPTION),
            row.date(EXPIRY),
            row.number(SUM_INSURED),
            row.number(PREMIUM),
            "PHP",
            row.text(PN)),
        new LegacyParties(
            row.text(CLIENT_CODE),
            row.text(CLIENT_NAME),
            row.text(CLIENT_NAME),
            row.text(INSURER),
            row.text(OFFICER),
            row.text(UNIT),
            row.text(SEGMENT),
            mortgagee),
        row.yes(URGENT),
        null);
  }
}
