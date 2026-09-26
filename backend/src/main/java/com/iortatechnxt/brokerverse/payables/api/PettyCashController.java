package com.iortatechnxt.brokerverse.payables.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.DatedReasonRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.DisbursementRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.DisbursementResponse;
import com.iortatechnxt.brokerverse.payables.api.dto.FundRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.FundResponse;
import com.iortatechnxt.brokerverse.payables.api.dto.ReimbursementRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.ReimbursementResponse;
import com.iortatechnxt.brokerverse.payables.service.PettyCashFundService;
import com.iortatechnxt.brokerverse.payables.service.PettyCashService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Petty cash funds, disbursement vouchers and reimbursement claims (imprest system). */
@RestController
@RequestMapping("/api/v1/payables/petty-cash")
public class PettyCashController {

  private final PettyCashFundService funds;
  private final PettyCashService service;

  /**
   * Creates the controller.
   *
   * @param funds fund service
   * @param service voucher service
   */
  public PettyCashController(PettyCashFundService funds, PettyCashService service) {
    this.funds = funds;
    this.service = service;
  }

  /**
   * Lists funds.
   *
   * @param companyId company
   * @return funds
   */
  @GetMapping("/funds")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<FundResponse> funds(@RequestParam Long companyId) {
    return funds.list(companyId).stream().map(FundResponse::from).toList();
  }

  /**
   * Creates a fund.
   *
   * @param request request
   * @return fund
   */
  @PostMapping("/funds")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public FundResponse createFund(@Valid @RequestBody FundRequest request) {
    return FundResponse.from(funds.create(request.toCommand()));
  }

  /**
   * Updates a fund.
   *
   * @param id id
   * @param request request
   * @return fund
   */
  @PutMapping("/funds/{id}")
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public FundResponse updateFund(@PathVariable Long id, @Valid @RequestBody FundRequest request) {
    return FundResponse.from(funds.update(id, request.toCommand()));
  }

  /**
   * Authorizes a fund.
   *
   * @param id id
   * @return fund
   */
  @PostMapping("/funds/{id}/authorize")
  @PreAuthorize(PayablesAccess.MASTER_AUTHORIZE)
  public FundResponse authorizeFund(@PathVariable Long id) {
    return FundResponse.from(funds.authorize(id));
  }

  /**
   * Establishes a fund (draws the imprest from the bank).
   *
   * @param id id
   * @param request establishment date
   * @return fund
   */
  @PostMapping("/funds/{id}/establish")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public FundResponse establish(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return FundResponse.from(funds.establish(id, request.date()));
  }

  /**
   * Lists disbursement vouchers of a fund.
   *
   * @param id fund
   * @param from from date
   * @param to to date
   * @return vouchers
   */
  @GetMapping("/funds/{id}/disbursements")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<DisbursementResponse> disbursements(
      @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.disbursements(id, ApiDefaults.from(from), ApiDefaults.to(to)).stream()
        .map(DisbursementResponse::from)
        .toList();
  }

  /**
   * Captures a disbursement voucher.
   *
   * @param id fund
   * @param request voucher
   * @return voucher
   */
  @PostMapping("/funds/{id}/disbursements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public DisbursementResponse disburse(
      @PathVariable Long id, @Valid @RequestBody DisbursementRequest request) {
    return DisbursementResponse.from(service.disburse(id, request.toValues()));
  }

  /**
   * Approves and posts a disbursement voucher.
   *
   * @param voucherId voucher
   * @return voucher
   */
  @PostMapping("/disbursements/{voucherId}/approve")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public DisbursementResponse approveDisbursement(@PathVariable Long voucherId) {
    return DisbursementResponse.from(service.approveDisbursement(voucherId));
  }

  /**
   * Rejects a disbursement voucher.
   *
   * @param voucherId voucher
   * @param request reason
   * @return voucher
   */
  @PostMapping("/disbursements/{voucherId}/reject")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public DisbursementResponse rejectDisbursement(
      @PathVariable Long voucherId, @Valid @RequestBody ReasonRequest request) {
    return DisbursementResponse.from(service.rejectDisbursement(voucherId, request.reason()));
  }

  /**
   * Lists reimbursement claims of a fund.
   *
   * @param id fund
   * @return claims
   */
  @GetMapping("/funds/{id}/reimbursements")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<ReimbursementResponse> reimbursements(@PathVariable Long id) {
    return service.reimbursements(id).stream().map(ReimbursementResponse::from).toList();
  }

  /**
   * Claims reimbursement.
   *
   * @param id fund
   * @param request claim
   * @return claim
   */
  @PostMapping("/funds/{id}/reimbursements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public ReimbursementResponse claim(
      @PathVariable Long id, @Valid @RequestBody ReimbursementRequest request) {
    return ReimbursementResponse.from(
        service.claimReimbursement(
            id, request.date(), request.disbursementIds(), request.narration()));
  }

  /**
   * Vouchers of a claim.
   *
   * @param claimId claim
   * @return vouchers
   */
  @GetMapping("/reimbursements/{claimId}/disbursements")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<DisbursementResponse> claimVouchers(@PathVariable Long claimId) {
    return service.claimVouchers(claimId).stream().map(DisbursementResponse::from).toList();
  }

  /**
   * Approves a claim (bank refills the box).
   *
   * @param claimId claim
   * @return claim
   */
  @PostMapping("/reimbursements/{claimId}/approve")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public ReimbursementResponse approveReimbursement(@PathVariable Long claimId) {
    return ReimbursementResponse.from(service.approveReimbursement(claimId));
  }

  /**
   * Rejects a claim.
   *
   * @param claimId claim
   * @param request reason
   * @return claim
   */
  @PostMapping("/reimbursements/{claimId}/reject")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public ReimbursementResponse rejectReimbursement(
      @PathVariable Long claimId, @Valid @RequestBody ReasonRequest request) {
    return ReimbursementResponse.from(service.rejectReimbursement(claimId, request.reason()));
  }
}
