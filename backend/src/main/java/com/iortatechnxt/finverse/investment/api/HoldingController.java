package com.iortatechnxt.finverse.investment.api;

import com.iortatechnxt.finverse.investment.api.dto.CouponReceiptRequest;
import com.iortatechnxt.finverse.investment.api.dto.FairValueRequest;
import com.iortatechnxt.finverse.investment.api.dto.HoldingRequest;
import com.iortatechnxt.finverse.investment.api.dto.HoldingResponse;
import com.iortatechnxt.finverse.investment.api.dto.RedemptionRequest;
import com.iortatechnxt.finverse.investment.api.dto.TransactionResponse;
import com.iortatechnxt.finverse.investment.domain.HoldingStatus;
import com.iortatechnxt.finverse.investment.service.HoldingEventService;
import com.iortatechnxt.finverse.investment.service.InvestmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for investment holdings. */
@RestController
@RequestMapping("/api/v1/investments/holdings")
public class HoldingController {

  private final InvestmentService service;
  private final HoldingEventService events;

  /**
   * Creates the controller.
   *
   * @param service holding capture and approval service
   * @param events coupon, maturity, sale and fair value service
   */
  public HoldingController(InvestmentService service, HoldingEventService events) {
    this.service = service;
    this.events = events;
  }

  /**
   * Searches holdings.
   *
   * @param companyId company
   * @param status status filter
   * @param portfolioId portfolio filter
   * @param q holding no. / security code / description fragment
   * @return holdings
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<HoldingResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) HoldingStatus status,
      @RequestParam(required = false) Long portfolioId,
      @RequestParam(required = false) String q) {
    return service.search(companyId, status, portfolioId, q).stream()
        .map(HoldingResponse::from)
        .toList();
  }

  /**
   * Gets a holding.
   *
   * @param id id
   * @return holding
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public HoldingResponse get(@PathVariable Long id) {
    return HoldingResponse.from(service.get(id));
  }

  /**
   * Transaction history of a holding.
   *
   * @param id id
   * @return transactions
   */
  @GetMapping("/{id}/transactions")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<TransactionResponse> transactions(@PathVariable Long id) {
    return service.transactions(id).stream().map(TransactionResponse::from).toList();
  }

  /**
   * Captures a holding (pending approval).
   *
   * @param request request
   * @return holding
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public HoldingResponse create(@Valid @RequestBody HoldingRequest request) {
    return HoldingResponse.from(service.create(request));
  }

  /**
   * Updates a holding awaiting approval.
   *
   * @param id id
   * @param request request
   * @return holding
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public HoldingResponse update(@PathVariable Long id, @Valid @RequestBody HoldingRequest request) {
    return HoldingResponse.from(service.update(id, request));
  }

  /**
   * Approves a holding and posts the purchase (checker).
   *
   * @param id id
   * @return holding
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public HoldingResponse approve(@PathVariable Long id) {
    return HoldingResponse.from(service.approve(id));
  }

  /**
   * Records a coupon / interest receipt.
   *
   * @param id id
   * @param request receipt
   * @return transaction
   */
  @PostMapping("/{id}/coupons")
  @PreAuthorize("hasAuthority('INVESTMENT_MANAGE')")
  public TransactionResponse coupon(
      @PathVariable Long id, @Valid @RequestBody CouponReceiptRequest request) {
    return TransactionResponse.from(events.receiveCoupon(id, request));
  }

  /**
   * Records the maturity of a holding.
   *
   * @param id id
   * @param request redemption
   * @return transaction
   */
  @PostMapping("/{id}/maturity")
  @PreAuthorize("hasAuthority('INVESTMENT_MANAGE')")
  public TransactionResponse mature(
      @PathVariable Long id, @Valid @RequestBody RedemptionRequest request) {
    return TransactionResponse.from(events.mature(id, request));
  }

  /**
   * Records the sale of a holding.
   *
   * @param id id
   * @param request sale
   * @return transaction
   */
  @PostMapping("/{id}/sale")
  @PreAuthorize("hasAuthority('INVESTMENT_MANAGE')")
  public TransactionResponse sell(
      @PathVariable Long id, @Valid @RequestBody RedemptionRequest request) {
    return TransactionResponse.from(events.sell(id, request));
  }

  /**
   * Remeasures a holding to fair value.
   *
   * @param id id
   * @param request valuation
   * @return transaction
   */
  @PostMapping("/{id}/fair-value")
  @PreAuthorize("hasAuthority('INVESTMENT_MANAGE')")
  public TransactionResponse fairValue(
      @PathVariable Long id, @Valid @RequestBody FairValueRequest request) {
    return TransactionResponse.from(events.remeasure(id, request));
  }
}
