package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The decision of a rate-scheme exception (BRPM.007, FR-PM-051) on its record, opened from My
 * Approvals: what the approver sees (the product and the scheme in force), the approval with an
 * optional comment (maker-checker through {@link CatalogRecords}) and the rejection with its
 * reason, by a PRODUCT_AUTHORIZE holder other than the requester; the requester is notified of the
 * decision.
 */
@Service
@Transactional
public class RateExceptionDecisions {

  /** Route of the exception record (My Approvals and the requester's notice open it). */
  public static final String LINK = "/catalog/rate-exceptions/";

  private static final String AUTHORIZE_PERMISSION = "PRODUCT_AUTHORIZE";

  private final RateSchemeExceptionService exceptions;
  private final ProductCatalogService catalog;
  private final CatalogRecords records;
  private final CurrentUser currentUser;
  private final ProductVersionQueryService versions;
  private final NotificationService notifications;
  private final UserDirectory users;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param exceptions exceptions
   * @param catalog products
   * @param records catalog maker-checker
   * @param currentUser current user
   * @param versions package versions
   * @param notifications in-app notices
   * @param users user names
   * @param audit audit trail
   * @param clock clock
   */
  public RateExceptionDecisions(
      RateSchemeExceptionService exceptions,
      ProductCatalogService catalog,
      CatalogRecords records,
      CurrentUser currentUser,
      ProductVersionQueryService versions,
      NotificationService notifications,
      UserDirectory users,
      AuditTrailService audit,
      Clock clock) {
    this.exceptions = exceptions;
    this.catalog = catalog;
    this.records = records;
    this.currentUser = currentUser;
    this.versions = versions;
    this.notifications = notifications;
    this.users = users;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The exception record with what the approver needs: the product name and the current scheme.
   *
   * @param reference reference number
   * @return exception and its context
   */
  @Transactional(readOnly = true)
  public Detail detail(String reference) {
    RateOverride e = exceptions.require(reference);
    RiskProduct product = catalog.requireProduct(e.getProductCode());
    ProductVersionView current = versions.current(e.getProductCode()).orElse(null);
    return new Detail(
        e,
        product.getName(),
        current == null ? null : current.versionNo(),
        current == null || current.rateScheme() == null
            ? null
            : current.rateScheme().defaultRate());
  }

  /**
   * An exception with the product name and the scheme in force today.
   *
   * @param exception exception
   * @param productName product name
   * @param currentVersionNo version in force, null when none
   * @param schemeRate scheme rate of that version in percent, null when per insurer or none
   */
  public record Detail(
      RateOverride exception,
      String productName,
      Integer currentVersionNo,
      BigDecimal schemeRate) {}

  /**
   * Approves a pending exception (PRODUCT_AUTHORIZE, never the requester); the quotation or account
   * is then priced and submitted with it.
   *
   * @param reference reference number
   * @param comment optional comment of the approver
   * @return the approved exception
   */
  public RateOverride approve(String reference, String comment) {
    requireDecider();
    RateOverride e = exceptions.require(reference);
    if (e.getRecordStatus() != RecordStatus.PENDING_AUTHORIZATION) {
      throw new BusinessRuleException(
          "RECORD_NOT_PENDING", "Rate exception " + reference + " is already decided");
    }
    // The requester is the creator: an exception is never changed, only decided.
    if (CurrentUser.sameUser(e.getCreatedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "A rate exception is decided by someone other than its requester");
    }
    records.authorize(CatalogKind.RATE_SCHEME_EXCEPTION, e.getId());
    e.recordDecision(currentUser.username(), clock.instant(), blankToNull(comment));
    notifyRequester(e, "approved", comment);
    return e;
  }

  /**
   * Rejects a pending exception (PRODUCT_AUTHORIZE, never the requester) with its reason; the
   * quotation cannot be submitted on the requested rate or version.
   *
   * @param reference reference number
   * @param reason why the exception is rejected
   * @return the rejected exception
   */
  public RateOverride reject(String reference, String reason) {
    requireDecider();
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "RATE_EXCEPTION_REASON_REQUIRED", "Enter the reason for the rejection");
    }
    RateOverride e = exceptions.require(reference);
    e.reject(currentUser.username(), clock.instant(), reason.strip());
    audit.record(
        CatalogKind.RATE_SCHEME_EXCEPTION.label(),
        reference,
        AuditAction.REJECT,
        "Rejected: " + reason.strip());
    notifyRequester(e, "rejected", reason);
    return e;
  }

  private void requireDecider() {
    if (!currentUser.hasAuthority(AUTHORIZE_PERMISSION)) {
      throw new AccessDeniedException("Not allowed to decide rate exceptions");
    }
  }

  private void notifyRequester(RateOverride e, String decision, String comment) {
    String by = users.displayName(currentUser.username());
    String body =
        e.getTransactionRef()
            + ": "
            + describe(e)
            + " "
            + decision
            + " by "
            + by
            + (comment == null || comment.isBlank() ? "." : " – " + comment.strip());
    notifications.notifyUser(
        e.getCreatedBy(),
        new Notice(
            "Rate exception " + e.getReferenceNo() + " " + decision,
            body,
            LINK + e.getReferenceNo(),
            CatalogKind.RATE_SCHEME_EXCEPTION.label(),
            e.getReferenceNo()));
  }

  /** "Rate 1.10% until 27-Oct-2026" or "Version 1 until ...". */
  private static String describe(RateOverride e) {
    String what =
        e.getRequestedVersionNo() != null
            ? "version " + e.getRequestedVersionNo()
            : "rate " + DisplayFormat.rate(e.getRequestedRate()) + "%";
    return what + " until " + DisplayFormat.date(e.getValidUntil());
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }
}
