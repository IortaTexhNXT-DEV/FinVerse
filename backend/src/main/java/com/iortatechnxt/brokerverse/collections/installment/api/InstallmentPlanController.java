package com.iortatechnxt.brokerverse.collections.installment.api;

import com.iortatechnxt.brokerverse.collections.installment.api.dto.PlanDtos.DueInstallmentResponse;
import com.iortatechnxt.brokerverse.collections.installment.api.dto.PlanDtos.GeneratedRequest;
import com.iortatechnxt.brokerverse.collections.installment.api.dto.PlanDtos.ManualRequest;
import com.iortatechnxt.brokerverse.collections.installment.api.dto.PlanDtos.PlanResponse;
import com.iortatechnxt.brokerverse.collections.installment.api.dto.PlanDtos.PolicyYearsRequest;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentRepository;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Installment plans of collection accounts (BRCLXN.053/054/058): list, detail, the installments due
 * and overdue, creation from the policy years of a multi-year account, generated or entered by the
 * collector ({@code CLX_BILLING}), allocation refresh and cancellation.
 */
@RestController
@RequestMapping("/api/v1/collections/plans")
public class InstallmentPlanController {

  private static final int MAX_PAGE = 200;

  private final InstallmentPlanService plans;
  private final InstallmentRepository installments;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param plans plans
   * @param installments installments (work list)
   * @param clock clock
   */
  public InstallmentPlanController(
      InstallmentPlanService plans, InstallmentRepository installments, Clock clock) {
    this.plans = plans;
    this.installments = installments;
    this.clock = clock;
  }

  /**
   * Plans, newest first.
   *
   * @param companyId company
   * @param status statuses
   * @param q plan, account, invoice, client or assured
   * @param page page
   * @param size size
   * @return plans
   */
  @GetMapping
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PageResponse<PlanResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<PlanStatus> status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        plans.search(companyId, status, q, pageOf(page, size, Sort.by(Sort.Direction.DESC, "id"))),
        PlanResponse::from);
  }

  /**
   * A plan with its installments.
   *
   * @param id plan
   * @return plan
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PlanResponse get(@PathVariable Long id) {
    return PlanResponse.detail(plans.get(id));
  }

  /**
   * Plans of an account, newest first, with their installments.
   *
   * @param arn account
   * @return plans
   */
  @GetMapping("/by-account")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public List<PlanResponse> byAccount(@RequestParam String arn) {
    return plans.forAccount(arn).stream().map(PlanResponse::detail).toList();
  }

  /**
   * Installments of live plans due up to a date and not paid, oldest first (BRCLXN.053).
   *
   * @param companyId company
   * @param until last due date (default today)
   * @param overdueOnly only the overdue ones
   * @param page page
   * @param size size
   * @return installments with their plan
   */
  @GetMapping("/installments/due")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PageResponse<DueInstallmentResponse> due(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate until,
      @RequestParam(defaultValue = "false") boolean overdueOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<InstallmentStatus> statuses =
        overdueOnly
            ? List.of(InstallmentStatus.OVERDUE)
            : List.of(
                InstallmentStatus.OVERDUE,
                InstallmentStatus.DUE,
                InstallmentStatus.PARTIAL,
                InstallmentStatus.NOT_DUE);
    return PageResponse.of(
        installments.due(
            companyId,
            PlanStatus.ACTIVE,
            statuses,
            until == null ? LocalDate.now(clock) : until,
            pageOf(page, size, Sort.unsorted())),
        DueInstallmentResponse::from);
  }

  /**
   * A plan over the policy years of a multi-year account (BRCLXN.058).
   *
   * @param request account and frequency
   * @return plan
   */
  @PostMapping("/policy-years")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlanResponse fromPolicyYears(@Valid @RequestBody PolicyYearsRequest request) {
    return PlanResponse.detail(
        plans.fromPolicyYears(
            request.companyId(), request.arn(), request.frequency(), request.remarks()));
  }

  /**
   * A plan splitting an invoice's outstanding premium (BRCLXN.053).
   *
   * @param request invoice, frequency, first due date and count
   * @return plan
   */
  @PostMapping("/generated")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlanResponse generate(@Valid @RequestBody GeneratedRequest request) {
    return PlanResponse.detail(
        plans.generate(
            request.companyId(),
            request.invoiceNo(),
            request.frequency(),
            request.firstDue(),
            request.count(),
            request.remarks()));
  }

  /**
   * A plan with installments entered by the collector.
   *
   * @param request invoice, frequency and installments
   * @return plan
   */
  @PostMapping("/manual")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlanResponse manual(@Valid @RequestBody ManualRequest request) {
    return PlanResponse.detail(
        plans.manual(
            request.companyId(),
            request.invoiceNo(),
            request.frequency(),
            request.toEntries(),
            request.remarks()));
  }

  /**
   * Allocates the ledger payments to the installments now.
   *
   * @param id plan
   * @return plan
   */
  @PostMapping("/{id}/refresh")
  @PreAuthorize("hasAnyAuthority('CLX_BILLING', 'CLX_WORK')")
  public PlanResponse refresh(@PathVariable Long id) {
    return PlanResponse.detail(plans.refresh(id));
  }

  /**
   * Cancels a plan.
   *
   * @param id plan
   * @param request reason
   * @return plan
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  public PlanResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return PlanResponse.detail(plans.cancel(id, request.reason()));
  }

  private static PageRequest pageOf(int page, int size, Sort sort) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE), sort);
  }
}
