package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest.Details;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationStatus;
import com.iortatechnxt.brokerverse.productmaint.service.DeactivationService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package deactivation requests (BDOI FRS FRPM.003.04, FRPM.003.06, FRPM.003.07, FRPM.017.01): the
 * Deactivation Request List with search and filter, the details, the request with effective date,
 * reason, remarks and approver, the approval or rejection with remarks, the withdrawal and the
 * reassignment of the approver. Supporting documents are attachments of the request.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/deactivations")
public class DeactivationController {

  private static final String REQUEST = "hasAnyAuthority('PKG_REQUEST', 'PRODUCT_MAINTAIN')";
  private static final String APPROVE = "hasAuthority('PKG_TSU_APPROVE')";
  private static final int MAX_PAGE = 100;

  private final DeactivationService service;
  private final DeactivationRequestRepository requests;

  /**
   * Creates the controller.
   *
   * @param service deactivation commands
   * @param requests deactivation requests (list)
   */
  public DeactivationController(
      DeactivationService service, DeactivationRequestRepository requests) {
    this.service = service;
    this.requests = requests;
  }

  /**
   * The route of the setting and the approvers to choose from.
   *
   * @return settings
   */
  @GetMapping("/settings")
  @PreAuthorize(PackageRequestController.VIEW)
  public SettingsView settings() {
    return new SettingsView(service.route(), service.approvers());
  }

  /**
   * The Deactivation Request List, newest first.
   *
   * @param q filters
   * @param page page
   * @param size size
   * @return page of requests
   */
  @GetMapping
  @PreAuthorize(PackageRequestController.VIEW)
  public PageResponse<DeactivationView> list(
      @ModelAttribute ListParams q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (q.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    Page<DeactivationRequest> found =
        requests.findAll(
            q.toSpecification(),
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    return PageResponse.of(found, DeactivationView::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return the request
   */
  @GetMapping("/{id}")
  @PreAuthorize(PackageRequestController.VIEW)
  public DeactivationView get(@PathVariable Long id) {
    return DeactivationView.from(service.require(id));
  }

  /**
   * Submits a deactivation request.
   *
   * @param body package, effective date, reason, remarks and approver
   * @return the request
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(REQUEST)
  public DeactivationView submit(@Valid @RequestBody SubmitBody body) {
    return DeactivationView.from(
        service.submit(
            body.companyId(),
            body.productCode(),
            new Details(body.effectiveDate(), body.reason(), body.remarks(), body.approver())));
  }

  /**
   * Approves a request (the selected approver).
   *
   * @param id request
   * @param body remarks
   * @return the request
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(APPROVE)
  public DeactivationView approve(@PathVariable Long id, @Valid @RequestBody RemarksBody body) {
    return DeactivationView.from(service.approve(id, body.remarks()));
  }

  /**
   * Rejects a request with the approval remarks (the selected approver).
   *
   * @param id request
   * @param body remarks (mandatory)
   * @return the request
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(APPROVE)
  public DeactivationView reject(@PathVariable Long id, @Valid @RequestBody RemarksBody body) {
    return DeactivationView.from(service.reject(id, body.remarks()));
  }

  /**
   * Withdraws a pending request (the requestor).
   *
   * @param id request
   * @return the request
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(REQUEST)
  public DeactivationView cancel(@PathVariable Long id) {
    return DeactivationView.from(service.cancel(id));
  }

  /**
   * Gives a pending request to another approver.
   *
   * @param id request
   * @param body new approver
   * @return the request
   */
  @PostMapping("/{id}/reassign")
  @PreAuthorize("hasAnyAuthority('WORK_ASSIGN', 'PKG_TSU_APPROVE')")
  public DeactivationView reassign(@PathVariable Long id, @Valid @RequestBody ReassignBody body) {
    return DeactivationView.from(service.reassign(id, body.approver()));
  }

  /**
   * The route and the approvers.
   *
   * @param route EFFECTIVE_DATED or RETIRE_REQUEST
   * @param approvers usernames of the approvers
   */
  public record SettingsView(String route, List<String> approvers) {}

  /**
   * The request a user submits.
   *
   * @param companyId company
   * @param productCode package
   * @param effectiveDate deactivation effective date (today or later)
   * @param reason reason code
   * @param remarks remarks (optional)
   * @param approver approver
   */
  public record SubmitBody(
      @NotNull Long companyId,
      @NotBlank @Size(max = 20) String productCode,
      @NotNull LocalDate effectiveDate,
      @NotBlank @Size(max = 40) String reason,
      @Size(max = 1000) String remarks,
      @NotBlank @Size(max = 50) String approver) {}

  /**
   * Approval remarks.
   *
   * @param remarks remarks
   */
  public record RemarksBody(@Size(max = 1000) String remarks) {}

  /**
   * New approver.
   *
   * @param approver username
   */
  public record ReassignBody(@NotBlank @Size(max = 50) String approver) {}

  /**
   * List filters.
   *
   * @param companyId company
   * @param status approval status
   * @param text request number, package code or name contains
   * @param from requested on or after
   * @param to requested on or before
   */
  public record ListParams(
      Long companyId,
      DeactivationStatus status,
      String text,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    Specification<DeactivationRequest> toSpecification() {
      return (root, query, cb) -> {
        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(root.get("companyId"), companyId));
        if (status != null) {
          where.add(cb.equal(root.get("status"), status));
        }
        if (text != null && !text.isBlank()) {
          String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
          where.add(
              cb.or(
                  cb.like(cb.lower(root.get("requestNo")), like),
                  cb.like(cb.lower(root.get("productCode")), like),
                  cb.like(cb.lower(root.get("packageName")), like)));
        }
        if (from != null) {
          where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), BusinessClock.startOf(from)));
        }
        if (to != null) {
          where.add(cb.lessThan(root.get("createdAt"), BusinessClock.startOf(to.plusDays(1))));
        }
        return cb.and(where.toArray(Predicate[]::new));
      };
    }
  }

  /**
   * A deactivation request.
   *
   * @param id id
   * @param requestNo request number
   * @param productCode package
   * @param versionNo version in force
   * @param packageName package name
   * @param effectiveDate deactivation effective date
   * @param reason reason code
   * @param remarks remarks
   * @param approver approver (username)
   * @param status approval status
   * @param statusLabel status name
   * @param packageExpiryDate package expiry date before the request
   * @param decisionRemarks approval remarks
   * @param decidedBy approver who decided
   * @param decidedAt approval date and time
   * @param expiryDate expiry date set on approval
   * @param requestedBy requestor
   * @param requestedAt request date and time
   */
  public record DeactivationView(
      Long id,
      String requestNo,
      String productCode,
      Integer versionNo,
      String packageName,
      LocalDate effectiveDate,
      String reason,
      String remarks,
      String approver,
      DeactivationStatus status,
      String statusLabel,
      LocalDate packageExpiryDate,
      String decisionRemarks,
      String decidedBy,
      Instant decidedAt,
      LocalDate expiryDate,
      String requestedBy,
      Instant requestedAt) {

    static DeactivationView from(DeactivationRequest r) {
      return new DeactivationView(
          r.getId(),
          r.getRequestNo(),
          r.getProductCode(),
          r.getVersionNo(),
          r.getPackageName(),
          r.getEffectiveDate(),
          r.getReason(),
          r.getRemarks(),
          r.getApprover(),
          r.getStatus(),
          r.getStatus().label(),
          r.getPackageEndDate(),
          r.getDecisionRemarks(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getExpiryDate(),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }
}
