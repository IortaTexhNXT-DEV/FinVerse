package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest.Details;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest.Target;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationStatus;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package deactivation requests (BDOI FRS FRPM.003.04, FRPM.003.06, FRPM.003.07 and FRPM.017.01):
 * the requestor gives the effective date (today or later), the reason, remarks and the approver;
 * the approver approves or rejects with remarks; on approval the package expiry date becomes the
 * later of the effective date and the approval date, and the daily step deactivates the package
 * after it. The route is chosen by the setting {@value #ROUTE_SETTING}: EFFECTIVE_DATED (this
 * request, BDOI's FRS) or RETIRE_REQUEST (a retirement package request).
 */
@Service
@Transactional
public class DeactivationService {

  /** Setting: the deactivation route. */
  public static final String ROUTE_SETTING = "PM_DEACTIVATION_ROUTE";

  /** Route of BDOI's FRS: an effective-dated deactivation request. */
  public static final String EFFECTIVE_DATED = "EFFECTIVE_DATED";

  /** Permission of the approvers of deactivation requests. */
  public static final String APPROVER_PERMISSION = "PKG_TSU_APPROVE";

  /** Audit entity type and attachment owner. */
  public static final String ENTITY = "PackageDeactivation";

  /** List of the reasons. */
  public static final String REASONS = "PKG_DEACTIVATION_REASON";

  /** Notification event: a request waits for the approver. */
  public static final String APPROVAL_EVENT = "PM_DEACTIVATION_APPROVAL";

  /** Notification event: the request was decided. */
  public static final String DECIDED_EVENT = "PM_DEACTIVATION_DECIDED";

  private static final String LINK = "/product-maintenance/deactivations?open=";

  private final DeactivationRequestRepository requests;
  private final ProductCatalogService catalog;
  private final ProductVersionQueryService versions;
  private final ProductVersionService versionCommands;
  private final PackageNumbers numbers;
  private final LovService lov;
  private final DeactivationNotices notices;
  private final UserDirectory directory;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;
  private final ProductMasterFeed feed;

  /**
   * Creates the service.
   *
   * @param requests deactivation requests
   * @param catalog products
   * @param versions package versions (version in force)
   * @param versionCommands package versions (expiry date)
   * @param numbers request numbers
   * @param lov lists of values (reasons)
   * @param notices notices to the approver and the requestor
   * @param directory users with a permission and their names
   * @param audit audit trail
   * @param currentUser current user
   * @param parameters business parameters (route)
   * @param clock clock
   * @param feed product master changes for the other BDOI systems
   */
  public DeactivationService(
      DeactivationRequestRepository requests,
      ProductCatalogService catalog,
      ProductVersionQueryService versions,
      ProductVersionService versionCommands,
      PackageNumbers numbers,
      LovService lov,
      DeactivationNotices notices,
      UserDirectory directory,
      AuditTrailService audit,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock,
      ProductMasterFeed feed) {
    this.requests = requests;
    this.catalog = catalog;
    this.versions = versions;
    this.versionCommands = versionCommands;
    this.numbers = numbers;
    this.lov = lov;
    this.notices = notices;
    this.directory = directory;
    this.audit = audit;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
    this.feed = feed;
  }

  /**
   * The deactivation route of the setting.
   *
   * @return EFFECTIVE_DATED or RETIRE_REQUEST
   */
  @Transactional(readOnly = true)
  public String route() {
    return parameters.text(ROUTE_SETTING, EFFECTIVE_DATED).strip();
  }

  /**
   * The users who may approve deactivation requests, except the current user.
   *
   * @return usernames
   */
  @Transactional(readOnly = true)
  public List<String> approvers() {
    String me = currentUser.username();
    return directory.usersWithPermission(APPROVER_PERMISSION).stream()
        .filter(u -> !CurrentUser.sameUser(u, me))
        .sorted()
        .toList();
  }

  /**
   * Creates a deactivation request after the checks of FRPM.003.04 and tells the approver.
   *
   * @param companyId company
   * @param productCode package
   * @param details effective date, reason, remarks and approver
   * @return the request
   */
  public DeactivationRequest submit(Long companyId, String productCode, Details details) {
    if (!EFFECTIVE_DATED.equals(route())) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_ROUTE",
          "Packages are deactivated through a retirement package request (setting "
              + ROUTE_SETTING
              + ")");
    }
    RiskProduct product = catalog.requireProduct(productCode);
    if (product.getLifecycleStatus() != ProductLifecycle.ACTIVE) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_NOT_ACTIVE", "Only an active package can be deactivated");
    }
    pending(productCode)
        .ifPresent(
            open -> {
              throw new BusinessRuleException(
                  "PKG_DEACTIVATION_PENDING",
                  "Package "
                      + productCode
                      + " already has the pending deactivation request "
                      + open.getRequestNo());
            });
    checkDetails(details);
    Optional<ProductVersionView> current = versions.current(productCode);
    Target target =
        new Target(
            productCode,
            current.map(ProductVersionView::versionNo).orElse(null),
            product.getName(),
            current.map(v -> v.dates().packageEndDate()).orElse(null));
    DeactivationRequest saved =
        requests.save(
            new DeactivationRequest(companyId, numbers.deactivation(), target, details.trimmed()));
    audit.record(
        ENTITY,
        saved.getRequestNo(),
        AuditAction.SUBMIT,
        "Deactivation of package "
            + productCode
            + " requested from "
            + saved.getEffectiveDate()
            + " for approval by "
            + directory.displayName(saved.getApprover()));
    notices.forApproval(saved, LINK + saved.getId());
    return saved;
  }

  private void checkDetails(Details d) {
    LocalDate today = BusinessClock.today(clock);
    if (d.effectiveDate() == null || d.effectiveDate().isBefore(today)) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_DATE", "The deactivation effective date must be today or later");
    }
    if (d.reason() == null || d.reason().isBlank()) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_REASON", "Select the reason for deactivation");
    }
    lov.requireValid(REASONS, d.reason().strip(), today);
    checkApprover(d.approver());
  }

  private void checkApprover(String approver) {
    if (approver == null || !approvers().contains(approver.strip())) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_APPROVER",
          "Select an approver who may approve package deactivation requests");
    }
  }

  /**
   * Approves a request: sets the package expiry date and tells the requestor.
   *
   * @param id request
   * @param remarks approval remarks (optional)
   * @return the request
   */
  public DeactivationRequest approve(Long id, String remarks) {
    DeactivationRequest r = requireForApprover(id);
    Instant now = clock.instant();
    LocalDate expiry = r.approve(currentUser.username(), remarks, now, BusinessClock.today(clock));
    if (r.getVersionNo() != null) {
      versionCommands.deactivateOn(r.getProductCode(), r.getVersionNo(), expiry, r.getRequestNo());
    } else {
      applyIfDue(r, BusinessClock.today(clock));
    }
    audit.recordChange(
        ENTITY,
        r.getRequestNo(),
        AuditAction.AUTHORIZE,
        "Deactivation approved; package expiry date " + expiry,
        r.getPackageEndDate() == null ? null : r.getPackageEndDate().toString(),
        expiry.toString());
    feed.publish(
        new ProductMasterFeed.ProductMasterChange(
            r.getProductCode(), r.getVersionNo(), "RETIRED", expiry, r.getRequestNo()));
    notices.decided(r, LINK + r.getId());
    return r;
  }

  /**
   * Rejects a request with the approval remarks; the package stays active.
   *
   * @param id request
   * @param remarks approval remarks (mandatory)
   * @return the request
   */
  public DeactivationRequest reject(Long id, String remarks) {
    DeactivationRequest r = requireForApprover(id);
    r.reject(currentUser.username(), remarks, clock.instant());
    audit.record(ENTITY, r.getRequestNo(), AuditAction.REJECT, "Deactivation rejected: " + remarks);
    notices.decided(r, LINK + r.getId());
    return r;
  }

  /**
   * Withdraws a pending request (the requestor only).
   *
   * @param id request
   * @return the request
   */
  public DeactivationRequest cancel(Long id) {
    DeactivationRequest r = require(id);
    if (!CurrentUser.sameUser(r.getCreatedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_REQUESTOR", "Only the requestor can withdraw the request");
    }
    r.cancel(currentUser.username(), clock.instant());
    audit.record(ENTITY, r.getRequestNo(), AuditAction.CLOSE, "Deactivation request withdrawn");
    return r;
  }

  /**
   * Gives a pending request to another approver and tells both (FRPM.017.01).
   *
   * @param id request
   * @param approver new approver
   * @return the request
   */
  public DeactivationRequest reassign(Long id, String approver) {
    DeactivationRequest r = require(id);
    String before = r.getApprover();
    if (approver == null
        || CurrentUser.sameUser(before, approver)
        || !directory.usersWithPermission(APPROVER_PERMISSION).contains(approver)) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_APPROVER",
          "Select another approver who may approve package deactivation requests");
    }
    r.reassign(approver);
    audit.recordChange(
        ENTITY, r.getRequestNo(), AuditAction.UPDATE, "Approver reassigned", before, approver);
    notices.reassigned(r, before, LINK + r.getId());
    return r;
  }

  /**
   * Deactivates the products without a package version whose approved expiry date has passed (the
   * daily step deactivates the package versions).
   *
   * @param date business date
   * @return products deactivated
   */
  public int applyDue(LocalDate date) {
    int done = 0;
    for (DeactivationRequest r :
        requests.findAll(
            (root, q, cb) ->
                cb.and(
                    cb.equal(root.get("status"), DeactivationStatus.APPROVED),
                    cb.isNull(root.get("versionNo")),
                    cb.lessThan(root.get("expiryDate"), date)))) {
      if (applyIfDue(r, date.minusDays(1))) {
        done++;
      }
    }
    return done;
  }

  private boolean applyIfDue(DeactivationRequest r, LocalDate lastDay) {
    RiskProduct product = catalog.requireProduct(r.getProductCode());
    if (r.getExpiryDate().isAfter(lastDay)
        || product.getLifecycleStatus() != ProductLifecycle.ACTIVE) {
      return false;
    }
    product.changeLifecycle(ProductLifecycle.RETIRED);
    audit.record(
        ENTITY,
        r.getRequestNo(),
        AuditAction.UPDATE,
        "Product " + r.getProductCode() + " deactivated on its expiry date " + r.getExpiryDate());
    return true;
  }

  /**
   * The pending request of a package, if any.
   *
   * @param productCode package
   * @return pending request
   */
  @Transactional(readOnly = true)
  public Optional<DeactivationRequest> pending(String productCode) {
    return requests
        .findByProductCodeAndStatusInOrderByIdDesc(productCode, List.of(DeactivationStatus.PENDING))
        .stream()
        .findFirst();
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @Transactional(readOnly = true)
  public DeactivationRequest require(Long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Deactivation request", id));
  }

  private DeactivationRequest requireForApprover(Long id) {
    DeactivationRequest r = require(id);
    if (!CurrentUser.sameUser(r.getApprover(), currentUser.username())) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_APPROVER", "Only the selected approver can decide this request");
    }
    return r;
  }
}
