package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.domain.SettlementOr;
import com.iortatechnxt.brokerverse.cashiering.service.SettlementOrService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The commission and incentive ORs of insurer settlements waiting for the approval of Disbursement
 * (FRS.CSH.07.01.01; Appendix R, C12), and issuing again an OR that could not be issued.
 */
@RestController
@RequestMapping("/api/v1/cashiering/settlement-ors")
public class SettlementOrController {

  private final SettlementOrService ors;

  /**
   * Creates the controller.
   *
   * @param ors settlement ORs
   */
  public SettlementOrController(SettlementOrService ors) {
    this.ors = ors;
  }

  /**
   * The settlement ORs of a company, latest first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return ORs
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<SettlementOrResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        ors.list(companyId, CashAccess.page(page, size)), SettlementOrResponse::from);
  }

  /**
   * Issues again an OR that could not be issued.
   *
   * @param id settlement OR
   * @return the settlement OR
   */
  @PostMapping("/{id}/issue")
  @PreAuthorize(CashAccess.RECEIPT)
  public SettlementOrResponse issueAgain(@PathVariable Long id) {
    return SettlementOrResponse.from(ors.issueAgain(id));
  }

  /**
   * A settlement OR.
   *
   * @param id id
   * @param sourceModule module that asked
   * @param sourceRef its reference
   * @param awaitRef payment request waited for
   * @param orType OR type
   * @param payeeName payee
   * @param currency currency
   * @param amount gross
   * @param status PENDING, ISSUED, CANCELLED or FAILED
   * @param receiptNo OR number
   * @param message what happened
   * @param createdAt kept at
   * @param issuedAt issued at
   */
  public record SettlementOrResponse(
      Long id,
      String sourceModule,
      String sourceRef,
      String awaitRef,
      String orType,
      String payeeName,
      String currency,
      BigDecimal amount,
      String status,
      String receiptNo,
      String message,
      Instant createdAt,
      Instant issuedAt) {

    static SettlementOrResponse from(SettlementOr o) {
      return new SettlementOrResponse(
          o.getId(),
          o.getSourceModule(),
          o.getSourceRef(),
          o.getAwaitRef(),
          o.getOrType(),
          o.getPayeeName(),
          o.getCurrency(),
          o.getAmount(),
          o.getStatus(),
          o.getReceiptNo(),
          o.getMessage(),
          o.getCreatedAt(),
          o.getIssuedAt());
    }
  }
}
