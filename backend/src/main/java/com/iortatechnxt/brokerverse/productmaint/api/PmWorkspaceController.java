package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.Counts;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.DrillPage;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.DrillRow;
import com.iortatechnxt.brokerverse.productmaint.service.PmDashboardQuery.Kpi;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery.MatrixPage;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMatrixQuery.MatrixRow;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery.QuotationPage;
import com.iortatechnxt.brokerverse.productmaint.service.QuotationRequestQuery.QuotationRow;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Product Maintenance workspace of BDOI's FRS: the dashboard KPIs and their drill-down
 * (FRPM.001.01), the Product Matrix with its Active, Expiring and Expired tabs (FRPM.002.02,
 * FRPM.003.01) and the Quotation Request List (FRPM.002.02, FRPM.005.01).
 */
@RestController
@RequestMapping("/api/v1/product-maintenance")
public class PmWorkspaceController {

  private final PmDashboardQuery dashboard;
  private final ProductMatrixQuery matrix;
  private final QuotationRequestQuery quotations;
  private final CurrentUser currentUser;
  private final UserDirectory directory;

  /**
   * Creates the controller.
   *
   * @param dashboard dashboard reads
   * @param matrix Product Matrix reads
   * @param quotations quotation request reads
   * @param currentUser current user (archive permission)
   * @param directory users (TSU officers of the filter)
   */
  public PmWorkspaceController(
      PmDashboardQuery dashboard,
      ProductMatrixQuery matrix,
      QuotationRequestQuery quotations,
      CurrentUser currentUser,
      UserDirectory directory) {
    this.dashboard = dashboard;
    this.matrix = matrix;
    this.quotations = quotations;
    this.currentUser = currentUser;
    this.directory = directory;
  }

  /**
   * The TSU officers of the TSU Officer filter.
   *
   * @return usernames, sorted
   */
  @GetMapping("/dashboard/officers")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<String> officers() {
    return directory.usersWithPermission("PKG_NEGOTIATE").stream().sorted().toList();
  }

  /**
   * The KPI counts for the filters.
   *
   * @param q filters
   * @return counts with the KPI labels
   */
  @GetMapping("/dashboard")
  @PreAuthorize(PackageRequestController.VIEW)
  public DashboardView dashboard(@ModelAttribute DashboardParams q) {
    Counts counts = dashboard.counts(q.toFilter());
    return new DashboardView(
        counts,
        PmDashboardQuery.KPIS.stream().map(k -> new KpiLabel(k.name(), k.label())).toList());
  }

  /**
   * The drill-down rows of one KPI, oldest first.
   *
   * @param q filters
   * @param kpi KPI
   * @param page page
   * @param size size
   * @return page of rows
   */
  @GetMapping("/dashboard/rows")
  @PreAuthorize(PackageRequestController.VIEW)
  public PageResponse<DrillRow> drillDown(
      @ModelAttribute DashboardParams q,
      @RequestParam Kpi kpi,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    int limit = Math.min(Math.max(size, 1), PmDashboardQuery.MAX_PAGE);
    DrillPage found = dashboard.rows(q.toFilter(), kpi, page, limit);
    return page(found.rows(), page, limit, found.total());
  }

  /**
   * The approvers a maker may name on a Product Matrix record (BDOI FRS FRPM.003.02).
   *
   * @return usernames, sorted
   */
  @GetMapping("/product-approvers")
  @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
  public List<String> productApprovers() {
    return directory.usersWithPermission("PRODUCT_AUTHORIZE").stream().sorted().toList();
  }

  /**
   * One page of the Product Matrix.
   *
   * @param q tab (ACTIVE, EXPIRING or EXPIRED), filters and sort
   * @param page page
   * @param size size
   * @return page of rows
   */
  @GetMapping("/matrix")
  @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
  public MatrixView matrix(
      @ModelAttribute MatrixParams q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    requireArchiveFor(q.tab());
    int limit = Math.min(Math.max(size, 1), ProductMatrixQuery.MAX_PAGE);
    MatrixPage found = matrix.search(q.toFilter(), page, limit);
    return new MatrixView(page(found.rows(), page, limit, found.total()), found.noticeDays());
  }

  /**
   * One page of the Quotation Request List.
   *
   * @param q filters and sort
   * @param page page
   * @param size size
   * @return page of rows
   */
  @GetMapping("/quotation-requests")
  @PreAuthorize(PackageRequestController.VIEW)
  public PageResponse<QuotationRow> quotationRequests(
      @ModelAttribute QuotationParams q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (q.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    int limit = Math.min(Math.max(size, 1), QuotationRequestQuery.MAX_PAGE);
    QuotationPage found = quotations.search(q.toFilter(), page, limit);
    return page(found.rows(), page, limit, found.total());
  }

  private void requireArchiveFor(String tab) {
    if ("EXPIRED".equals(tab) && !currentUser.hasAuthority("PRODUCT_ARCHIVE_VIEW")) {
      throw new AccessDeniedException("The expired products need the archive permission");
    }
  }

  private static <T> PageResponse<T> page(List<T> rows, int page, int size, long total) {
    return new PageResponse<>(
        rows, Math.max(page, 0), size, total, (int) ((total + size - 1) / size));
  }

  /**
   * Dashboard filters.
   *
   * @param companyId company
   * @param from period from
   * @param to period to
   * @param tsuOfficer TSU officer
   * @param lineCode product line
   * @param packaged package type (true package, false non-package)
   */
  public record DashboardParams(
      Long companyId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      String tsuOfficer,
      String lineCode,
      Boolean packaged) {

    PmDashboardQuery.Filter toFilter() {
      if (companyId == null) {
        throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
      }
      if (from != null && to != null && to.isBefore(from)) {
        throw new BusinessRuleException(
            "DATE_RANGE", "The Period To date must be on or after the Period From date");
      }
      return new PmDashboardQuery.Filter(companyId, from, to, tsuOfficer, lineCode, packaged);
    }
  }

  /**
   * Product Matrix filters.
   *
   * @param tab ACTIVE (default), EXPIRING or EXPIRED
   * @param text package code, name or description contains
   * @param lineCode line of insurance
   * @param packaged true for packages, false for non-package products
   * @param sort sort key
   * @param direction asc or desc
   */
  public record MatrixParams(
      String tab, String text, String lineCode, Boolean packaged, String sort, String direction) {

    /**
     * The filter of the query.
     *
     * @return filter
     */
    public ProductMatrixQuery.Filter toFilter() {
      return new ProductMatrixQuery.Filter(tab, text, lineCode, packaged, sort, direction);
    }
  }

  /**
   * Quotation request filters.
   *
   * @param companyId company
   * @param text contains
   * @param lineCode product line
   * @param status status code
   * @param tsuUser assigned TSU user
   * @param open open (true) or closed (false) requests
   * @param sort sort key
   * @param direction asc or desc
   */
  public record QuotationParams(
      Long companyId,
      String text,
      String lineCode,
      String status,
      String tsuUser,
      Boolean open,
      String sort,
      String direction) {

    QuotationRequestQuery.Filter toFilter() {
      return new QuotationRequestQuery.Filter(
          companyId, text, lineCode, status, tsuUser, open, sort, direction);
    }
  }

  /**
   * Dashboard counts with the KPI labels.
   *
   * @param counts counts
   * @param kpis KPI codes and labels in screen order
   */
  public record DashboardView(Counts counts, List<KpiLabel> kpis) {}

  /**
   * A KPI code and its label.
   *
   * @param code KPI
   * @param label label
   */
  public record KpiLabel(String code, String label) {}

  /**
   * A page of the Product Matrix.
   *
   * @param page rows
   * @param noticeDays notice period of the Expiring Products tab
   */
  public record MatrixView(PageResponse<MatrixRow> page, int noticeDays) {}
}
