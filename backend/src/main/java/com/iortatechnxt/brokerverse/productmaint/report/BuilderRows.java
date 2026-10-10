package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService.CaseFacts;
import com.iortatechnxt.brokerverse.productmaint.service.PmStatusNames;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery.MatrixRow;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery.QuotationPage;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery.QuotationRow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * The records of the report builder's sources (BDOI FRS FRPM.020.01), each as a value per {@link
 * BuilderField}: the quotation requests, the package requests and the products of the Product
 * Matrix of a company.
 */
@Component
public class BuilderRows {

  /** Quotation requests. */
  public static final String QUOTATION = "QUOTATION_REQUESTS";

  /** Package requests. */
  public static final String PACKAGE = "PACKAGE_REQUESTS";

  /** Products of the Product Matrix. */
  public static final String PRODUCT = "PRODUCTS";

  private static final int MAX_ROWS = 5000;

  private final QuotationRequestQuery quotations;
  private final PackageQueryService packages;
  private final ProductMatrixQuery matrix;
  private final UserDisplayNames names;
  private final Clock clock;

  /**
   * Creates the reader.
   *
   * @param quotations quotation requests
   * @param packages package requests
   * @param matrix Product Matrix
   * @param names user names
   * @param clock clock (aging)
   */
  public BuilderRows(
      QuotationRequestQuery quotations,
      PackageQueryService packages,
      ProductMatrixQuery matrix,
      UserDisplayNames names,
      Clock clock) {
    this.quotations = quotations;
    this.packages = packages;
    this.matrix = matrix;
    this.names = names;
    this.clock = clock;
  }

  /**
   * The records of a source.
   *
   * @param source QUOTATION_REQUESTS, PACKAGE_REQUESTS or PRODUCTS
   * @param companyId company
   * @param lineCode line of insurance, null for every line
   * @return records
   */
  public List<Map<BuilderField, Object>> of(String source, Long companyId, String lineCode) {
    return switch (source) {
      case PACKAGE -> packageRows(companyId, lineCode);
      case PRODUCT -> productRows(lineCode);
      default -> quotationRows(companyId, lineCode);
    };
  }

  private List<Map<BuilderField, Object>> quotationRows(Long companyId, String lineCode) {
    List<Map<BuilderField, Object>> out = new ArrayList<>();
    QuotationRequestQuery.Filter f =
        new QuotationRequestQuery.Filter(companyId, null, lineCode, null, null, null, null, "asc");
    int page = 0;
    QuotationPage found;
    do {
      found = quotations.search(f, page++, QuotationRequestQuery.MAX_PAGE);
      found.rows().forEach(r -> out.add(quotation(r)));
    } while (out.size() < found.total() && out.size() < MAX_ROWS && !found.rows().isEmpty());
    return out;
  }

  private Map<BuilderField, Object> quotation(QuotationRow r) {
    Map<BuilderField, Object> v = new EnumMap<>(BuilderField.class);
    v.put(BuilderField.REFERENCE, r.requestNo());
    v.put(BuilderField.REQUEST_DATE, r.requestDate());
    v.put(BuilderField.CLIENT, r.assuredName());
    v.put(BuilderField.LINE, r.productLine());
    v.put(BuilderField.OFFICER, names.displayName(r.accountOfficer()));
    v.put(BuilderField.TSU_USER, r.tsuUser() == null ? null : names.displayName(r.tsuUser()));
    v.put(BuilderField.STATUS, r.status());
    v.put(
        BuilderField.SUBMITTED,
        r.submittedAt() == null ? null : BusinessClock.dateOf(r.submittedAt()));
    v.put(BuilderField.EFFECTIVE, r.effectiveDate());
    v.put(BuilderField.EXPIRY, r.expiryDate());
    v.put(BuilderField.AGING, r.agingDays());
    v.put(BuilderField.TYPE, r.businessType());
    return v;
  }

  private List<Map<BuilderField, Object>> packageRows(Long companyId, String lineCode) {
    List<PackageRequest> found =
        packages
            .search(
                new PackageQueryService.Search(companyId, null, null, null, null, null, null, null),
                PageRequest.of(0, MAX_ROWS, Sort.by("createdAt", "id")))
            .getContent()
            .stream()
            .filter(p -> lineCode == null || lineCode.equals(p.getLineCode()))
            .toList();
    Map<Long, CaseFacts> facts =
        packages.caseFacts(found.stream().map(PackageRequest::getId).toList());
    LocalDate today = BusinessClock.today(clock);
    return found.stream().map(p -> packageRow(p, facts.get(p.getId()), today)).toList();
  }

  private Map<BuilderField, Object> packageRow(PackageRequest p, CaseFacts f, LocalDate today) {
    Map<BuilderField, Object> v = new EnumMap<>(BuilderField.class);
    v.put(BuilderField.REFERENCE, p.getRequestNo());
    v.put(BuilderField.REQUEST_DATE, BusinessClock.dateOf(p.getCreatedAt()));
    v.put(BuilderField.CLIENT, p.getClientName() == null ? p.getTitle() : p.getClientName());
    v.put(BuilderField.LINE, p.getLineCode());
    v.put(BuilderField.PRODUCT, p.getTargetProductCode());
    v.put(BuilderField.OFFICER, names.displayName(p.getCreatedBy()));
    v.put(
        BuilderField.TSU_USER,
        f == null || f.assignee() == null ? null : names.displayName(f.assignee()));
    v.put(
        BuilderField.STATUS,
        PmStatusNames.packageRequest(p.getStatus(), f == null ? null : f.lastAction()));
    v.put(
        BuilderField.SUBMITTED,
        p.getMilestones().getSubmittedAt() == null
            ? null
            : BusinessClock.dateOf(p.getMilestones().getSubmittedAt()));
    v.put(BuilderField.EXPIRY, p.getPackageEndDate());
    v.put(
        BuilderField.AGING,
        f == null || f.stageEnteredAt() == null
            ? null
            : ChronoUnit.DAYS.between(BusinessClock.dateOf(f.stageEnteredAt()), today));
    v.put(BuilderField.TYPE, p.getRequestType().name());
    return v;
  }

  private List<Map<BuilderField, Object>> productRows(String lineCode) {
    return matrix
        .all(new ProductMatrixQuery.Filter("ACTIVE", null, lineCode, null, null, null))
        .stream()
        .map(BuilderRows::product)
        .toList();
  }

  private static Map<BuilderField, Object> product(MatrixRow r) {
    Map<BuilderField, Object> v = new EnumMap<>(BuilderField.class);
    v.put(BuilderField.REFERENCE, r.productCode());
    v.put(BuilderField.LINE, r.lineOfInsurance());
    v.put(BuilderField.SUB_LINE, r.subLine());
    v.put(BuilderField.PRODUCT, r.packageName());
    v.put(BuilderField.STATUS, r.status());
    v.put(BuilderField.EFFECTIVE, r.effectiveDate());
    v.put(BuilderField.EXPIRY, r.expiryDate());
    v.put(BuilderField.INSURERS, r.insurers());
    v.put(BuilderField.TYPE, r.productType());
    return v;
  }
}
