package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride.Request;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverrideRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate-scheme exceptions (BRPM.007; PRODUCT_MAINTENANCE_DESIGN sections 4.2 and 9.1): "a
 * non-current rate triggers an approval workflow and is logged". A requester asks to price one
 * quotation or account on a non-current package version or on another item rate; a
 * PRODUCT_AUTHORIZE holder other than the requester decides it on the exception record opened from
 * My Approvals: approves it (maker-checker through {@link CatalogRecords}) with an optional
 * comment, or rejects it with a reason; the requester is notified of the decision. Rating accepts
 * the deviation only with the approved reference.
 */
@Service
@Transactional
public class RateSchemeExceptionService {

  /** Error code of a deviation from the current scheme without an approved exception. */
  public static final String NOT_CURRENT = "RATE_SCHEME_NOT_CURRENT";

  /** Route of the exception record (My Approvals and the requester's notice open it). */
  public static final String LINK = "/catalog/rate-exceptions/";

  private static final int DEFAULT_VALIDITY_DAYS = 30;
  private static final String AUTHORIZE_PERMISSION = "PRODUCT_AUTHORIZE";

  private final RateOverrideRepository exceptions;
  private final ProductCatalogService catalog;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;
  private final Deciding deciding;

  /**
   * Who decides an exception and what they see: the maker-checker of the catalog, the current user,
   * the version in force (scheme rate), the requester's notice and user names.
   *
   * @param records catalog maker-checker
   * @param currentUser current user
   * @param versions package versions
   * @param notifications in-app notices
   * @param users user names
   */
  public record Deciding(
      CatalogRecords records,
      CurrentUser currentUser,
      ProductVersionQueryService versions,
      NotificationService notifications,
      UserDirectory users) {}

  /**
   * Creates the service.
   *
   * @param exceptions exceptions
   * @param catalog products
   * @param numbers reference numbers
   * @param audit audit trail
   * @param clock clock
   * @param records catalog maker-checker
   * @param currentUser current user
   * @param versions package versions
   * @param notifications in-app notices
   * @param users user names
   */
  @SuppressWarnings("java:S107") // collaborators of the request and the decision
  public RateSchemeExceptionService(
      RateOverrideRepository exceptions,
      ProductCatalogService catalog,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock,
      CatalogRecords records,
      CurrentUser currentUser,
      ProductVersionQueryService versions,
      NotificationService notifications,
      UserDirectory users) {
    this.exceptions = exceptions;
    this.catalog = catalog;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
    this.deciding = new Deciding(records, currentUser, versions, notifications, users);
  }

  /**
   * The exception record with what the approver needs: the product name and the current scheme.
   *
   * @param reference reference number
   * @return exception and its context
   */
  @Transactional(readOnly = true)
  public Detail detail(String reference) {
    RateOverride e = require(reference);
    RiskProduct product = catalog.requireProduct(e.getProductCode());
    ProductVersionView current = deciding.versions().current(e.getProductCode()).orElse(null);
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
    RateOverride e = require(reference);
    if (CurrentUser.sameUser(e.getMaker(), deciding.currentUser().username())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "A rate exception is decided by someone other than its requester");
    }
    deciding.records().authorize(CatalogKind.RATE_SCHEME_EXCEPTION, e.getId());
    e.recordDecision(deciding.currentUser().username(), clock.instant(), blankToNull(comment));
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
    RateOverride e = require(reference);
    e.reject(deciding.currentUser().username(), clock.instant(), reason.strip());
    audit.record(
        CatalogKind.RATE_SCHEME_EXCEPTION.label(),
        reference,
        AuditAction.REJECT,
        "Rejected: " + reason.strip());
    notifyRequester(e, "rejected", reason);
    return e;
  }

  private void requireDecider() {
    if (!deciding.currentUser().hasAuthority(AUTHORIZE_PERMISSION)) {
      throw new AccessDeniedException("Not allowed to decide rate exceptions");
    }
  }

  private void notifyRequester(RateOverride e, String decision, String comment) {
    String by = deciding.users().displayName(deciding.currentUser().username());
    String body =
        e.getTransactionRef()
            + ": "
            + describe(e)
            + " "
            + decision
            + " by "
            + by
            + (comment == null || comment.isBlank() ? "." : " – " + comment.strip());
    deciding
        .notifications()
        .notifyUser(
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

  /**
   * Requests an exception, pending authorisation (it appears in My Approvals).
   *
   * @param request product, purpose, version or rate, transaction, reason and validity (null for 30
   *     days)
   * @return the exception with its reference number
   */
  public RateOverride request(Request request) {
    requireComplete(request);
    LocalDate today = BusinessClock.today(clock);
    LocalDate validUntil =
        request.validUntil() != null ? request.validUntil() : today.plusDays(DEFAULT_VALIDITY_DAYS);
    if (validUntil.isBefore(today)) {
      throw new BusinessRuleException(
          "RATE_EXCEPTION_VALIDITY_PAST", "The validity date cannot be in the past");
    }
    String reference = numbers.next("RSE-" + today.getYear());
    RateOverride saved =
        exceptions.save(
            new RateOverride(
                reference,
                new Request(
                    request.productCode(),
                    request.purpose() == null ? "NEW_BUSINESS" : request.purpose(),
                    request.requestedVersionNo(),
                    request.requestedRate(),
                    request.transactionRef(),
                    request.reason(),
                    validUntil)));
    audit.record(
        CatalogKind.RATE_SCHEME_EXCEPTION.label(),
        reference,
        AuditAction.CREATE,
        "Requested: " + saved.catalogDescription() + " - " + request.reason());
    return saved;
  }

  private void requireComplete(Request request) {
    RiskProduct product = catalog.requireProduct(request.productCode());
    if (!product.isPackaged()) {
      throw new BusinessRuleException(
          "PRODUCT_NOT_PACKAGED", "Only package rate schemes need an exception");
    }
    if (request.requestedVersionNo() == null && request.requestedRate() == null) {
      throw new BusinessRuleException(
          "RATE_EXCEPTION_INCOMPLETE", "Enter the version or the rate the exception is for");
    }
  }

  /**
   * Every exception, or those of one transaction, newest first.
   *
   * @param transactionRef quotation number or ARN, null for all
   * @return exceptions
   */
  @Transactional(readOnly = true)
  public List<RateOverride> list(String transactionRef) {
    return transactionRef == null
        ? exceptions.findAllByOrderByIdDesc()
        : exceptions.findByTransactionRefOrderByIdDesc(transactionRef);
  }

  /**
   * An exception by reference.
   *
   * @param reference reference number
   * @return exception
   */
  @Transactional(readOnly = true)
  public RateOverride require(String reference) {
    return exceptions
        .findByReferenceNo(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    CatalogKind.RATE_SCHEME_EXCEPTION.label(), reference));
  }

  /**
   * The approved exception a deviation needs (BRPM.007).
   *
   * @param reference exception reference, null when none was given
   * @param productCode risk code
   * @param versionNo non-current version used, null when the version is current
   * @param rate item rate used, null when the rate is the scheme rate
   * @return the approved exception
   */
  @Transactional(readOnly = true)
  public RateOverride requireApproved(
      String reference, String productCode, Integer versionNo, BigDecimal rate) {
    String what = versionNo != null ? "version " + versionNo : "rate " + rate.toPlainString() + "%";
    if (reference == null || reference.isBlank()) {
      throw new BusinessRuleException(
          NOT_CURRENT,
          "Package "
              + productCode
              + " is priced on the current rate scheme: "
              + what
              + " needs an approved rate exception");
    }
    return exceptions
        .findByReferenceNo(reference)
        .filter(e -> e.covers(productCode, BusinessClock.today(clock)))
        .filter(e -> approves(e, versionNo, rate))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    NOT_CURRENT,
                    "Rate exception "
                        + reference
                        + " does not approve "
                        + what
                        + " for "
                        + productCode));
  }

  private static boolean approves(RateOverride e, Integer versionNo, BigDecimal rate) {
    boolean version = versionNo == null || versionNo.equals(e.getRequestedVersionNo());
    boolean sameRate =
        rate == null || e.getRequestedRate() != null && e.getRequestedRate().compareTo(rate) == 0;
    return version && sameRate;
  }

  /**
   * The newest approved and still valid exception of a transaction (the quotation or account prices
   * with it once it is approved, BRPM.007).
   *
   * @param transactionRef quotation number or ARN
   * @param productCode risk code
   * @return exception reference, null when none
   */
  @Transactional(readOnly = true)
  public String latestApproved(String transactionRef, String productCode) {
    if (transactionRef == null) {
      return null;
    }
    LocalDate today = BusinessClock.today(clock);
    return exceptions.findByTransactionRefOrderByIdDesc(transactionRef).stream()
        .filter(e -> e.covers(productCode, today))
        .map(RateOverride::getReferenceNo)
        .findFirst()
        .orElse(null);
  }

  /**
   * The approved exception of a reference, if it is approved and still valid for the product.
   *
   * @param reference reference, may be null
   * @param productCode risk code
   * @return the exception, null when none applies
   */
  @Transactional(readOnly = true)
  public RateOverride approvedOrNull(String reference, String productCode) {
    if (reference == null || reference.isBlank()) {
      return null;
    }
    return exceptions
        .findByReferenceNo(reference)
        .filter(e -> e.covers(productCode, BusinessClock.today(clock)))
        .orElse(null);
  }
}
