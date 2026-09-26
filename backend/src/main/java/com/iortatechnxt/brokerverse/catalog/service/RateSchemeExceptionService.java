package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride.Request;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverrideRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate-scheme exceptions (BRPM.007; PRODUCT_MAINTENANCE_DESIGN sections 4.2 and 9.1): "a
 * non-current rate triggers an approval workflow and is logged". A requester asks to price one
 * quotation or account on a non-current package version or on another item rate; a
 * PRODUCT_AUTHORIZE holder other than the requester authorises it in My Approvals (maker-checker
 * through {@link CatalogRecords}); rating accepts the deviation only with the approved reference.
 */
@Service
@Transactional
public class RateSchemeExceptionService {

  /** Error code of a deviation from the current scheme without an approved exception. */
  public static final String NOT_CURRENT = "RATE_SCHEME_NOT_CURRENT";

  private static final int DEFAULT_VALIDITY_DAYS = 30;

  private final RateOverrideRepository exceptions;
  private final ProductCatalogService catalog;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param exceptions exceptions
   * @param catalog products
   * @param numbers reference numbers
   * @param audit audit trail
   * @param clock clock
   */
  public RateSchemeExceptionService(
      RateOverrideRepository exceptions,
      ProductCatalogService catalog,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.exceptions = exceptions;
    this.catalog = catalog;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
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
    LocalDate today = LocalDate.now(clock);
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
        .filter(e -> e.covers(productCode, LocalDate.now(clock)))
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
    LocalDate today = LocalDate.now(clock);
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
        .filter(e -> e.covers(productCode, LocalDate.now(clock)))
        .orElse(null);
  }
}
