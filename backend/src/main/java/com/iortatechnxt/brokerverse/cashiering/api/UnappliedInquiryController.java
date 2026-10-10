package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry.Filter;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry.Row;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The list of the unapplied payments with the filters and columns of FRS.CSH.06.01.03 / 06.01.04,
 * for Cashiering, Marketing (own marketing unit) and Commission Receivable users.
 */
@RestController
@RequestMapping("/api/v1/cashiering/unapplied-inquiry")
public class UnappliedInquiryController {

  private final UnappliedInquiry inquiry;

  /**
   * Creates the controller.
   *
   * @param inquiry the list
   */
  public UnappliedInquiryController(UnappliedInquiry inquiry) {
    this.inquiry = inquiry;
  }

  /**
   * Searches the unapplied payments.
   *
   * @param companyId company
   * @param params filters
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW + " or hasAuthority('CLX_UNAPPLIED_WORK')")
  public PageResponse<Row> search(
      @RequestParam Long companyId,
      Params params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        inquiry.search(companyId, params.filter(), CashAccess.page(page, size)), r -> r);
  }

  /**
   * Filters as request parameters.
   *
   * @param type unapplied payment type
   * @param from payment date from
   * @param to payment date to
   * @param insurer insurer code
   * @param client client code
   * @param assured part of the assured's name
   * @param account account reference
   * @param q part of the number, reference or payor
   * @param all true to list the fully applied payments too
   */
  public record Params(
      String type,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      String insurer,
      String client,
      String assured,
      String account,
      String q,
      Boolean all) {

    Filter filter() {
      return new Filter(
          type, from, to, insurer, client, assured, account, q, !Boolean.TRUE.equals(all));
    }
  }
}
