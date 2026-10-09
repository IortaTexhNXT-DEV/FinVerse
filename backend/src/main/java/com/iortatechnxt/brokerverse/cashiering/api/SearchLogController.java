package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.SearchLogRow;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The search log of the receipt inquiry (FRS.CSH.08.01.04), viewed by the Auditor and the
 * Cashiering Team Leader.
 */
@RestController
@RequestMapping("/api/v1/cashiering/search-log")
public class SearchLogController {

  private final ReceiptSearchService search;

  /**
   * Creates the controller.
   *
   * @param search the search and its log
   */
  public SearchLogController(ReceiptSearchService search) {
    this.search = search;
  }

  /**
   * The searches of a company.
   *
   * @param companyId company
   * @param user part of the user name or login
   * @param from from date
   * @param to to date
   * @param page page
   * @param size size
   * @return searches, latest first
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('AUDIT_VIEW', 'CASH_APPROVE', 'OPS_REPORT_VIEW')")
  public PageResponse<SearchLogRow> log(
      @RequestParam Long companyId,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        search.log(companyId, user, from, to, CashAccess.page(page, size)), r -> r);
  }
}
