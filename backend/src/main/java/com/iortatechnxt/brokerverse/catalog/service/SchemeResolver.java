package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SchemeTerms;
import com.iortatechnxt.brokerverse.catalog.domain.VersionInsurer;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueries;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides which rate scheme prices a transaction (BRPM.007; PRODUCT_MAINTENANCE_DESIGN section
 * 5.2). Non-packaged products and packages without versions use the product columns. For a
 * versioned package:
 *
 * <ul>
 *   <li>NEW_BUSINESS: the RELEASED version in force on the <b>transaction date</b> (the clock), not
 *       the period start; a product EXPIRED / RETIRED, or without a version in force, is refused
 *       ({@code PRODUCT_NOT_SELLABLE}); another version needs an approved exception;
 *   <li>RENEWAL: the version asked for when RELEASED or SUPERSEDED, else the current one;
 *   <li>ENDORSEMENT: the version of the original account (the one in force on the rating date for
 *       accounts created before versions existed).
 * </ul>
 *
 * The insurer of the query must be on the version's panel when it has one ({@code
 * INSURER_NOT_ON_PACKAGE}); the insurer's own rate and minimum premium win over the scheme's.
 */
@Component
@Transactional(readOnly = true)
public class SchemeResolver {

  private static final String NOT_SELLABLE = "PRODUCT_NOT_SELLABLE";

  private final ProductVersionQueries versions;
  private final RateSchemeExceptionService exceptions;
  private final Clock clock;

  /**
   * Creates the resolver.
   *
   * @param versions package versions
   * @param exceptions rate-scheme exceptions
   * @param clock clock (transaction date)
   */
  public SchemeResolver(
      ProductVersionQueries versions, RateSchemeExceptionService exceptions, Clock clock) {
    this.versions = versions;
    this.exceptions = exceptions;
    this.clock = clock;
  }

  /**
   * The scheme that prices a query.
   *
   * @param product product
   * @param query query (purpose, version, exception reference, insurer)
   * @param ratingDate date of the statutory rates (endorsement look-up of old accounts)
   * @return scheme
   */
  public Scheme resolve(RiskProduct product, RatingQuery query, LocalDate ratingDate) {
    if (!product.isPackaged() || !versions.isVersioned(product.getCode())) {
      return Scheme.of(product);
    }
    LocalDate today = LocalDate.now(clock);
    return switch (query.purpose()) {
      case RENEWAL ->
          Scheme.of(
              renewal(product, query.schemeVersion(), today), query.insurerCode(), null, false);
      case ENDORSEMENT ->
          Scheme.of(
              endorsement(product, query.schemeVersion(), ratingDate, today),
              query.insurerCode(),
              null,
              false);
      case NEW_BUSINESS -> newBusiness(product, query, today);
    };
  }

  private Scheme newBusiness(RiskProduct product, RatingQuery query, LocalDate today) {
    ProductVersion current = current(product, today);
    Integer wanted = wantedVersion(product, query);
    if (wanted == null || wanted.intValue() == current.getVersionNo()) {
      return Scheme.of(current, query.insurerCode(), null, false);
    }
    String overrideRef =
        exceptions
            .requireApproved(query.rateOverrideRef(), product.getCode(), wanted, null)
            .getReferenceNo();
    return Scheme.of(
        versions.require(product.getCode(), wanted), query.insurerCode(), overrideRef, true);
  }

  /**
   * Checks the item rates of a new-business query against the scheme (BRPM.007): an item rate other
   * than the scheme (or insurer) rate needs an approved exception for that rate, unless the version
   * still accepts manual rates (PQ10).
   *
   * @param scheme resolved scheme
   * @param query query
   * @return the exception reference used, the scheme's own when none was needed
   */
  public String requireSchemeRates(Scheme scheme, RatingQuery query) {
    String ref = scheme.overrideRef();
    for (BigDecimal rate : scheme.deviatingRates(query)) {
      ref =
          exceptions
              .requireApproved(query.rateOverrideRef(), query.productCode(), null, rate)
              .getReferenceNo();
    }
    return ref;
  }

  private Integer wantedVersion(RiskProduct product, RatingQuery query) {
    if (query.schemeVersion() != null) {
      return query.schemeVersion();
    }
    RateOverride approved = exceptions.approvedOrNull(query.rateOverrideRef(), product.getCode());
    return approved == null ? null : approved.getRequestedVersionNo();
  }

  private ProductVersion current(RiskProduct product, LocalDate today) {
    if (product.getLifecycleStatus() != ProductLifecycle.ACTIVE) {
      throw new BusinessRuleException(
          NOT_SELLABLE,
          "Package "
              + product.getCode()
              + " is "
              + product.getLifecycleStatus()
              + ": it is locked for new business");
    }
    return versions
        .inForceEntity(product.getCode(), today)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    NOT_SELLABLE,
                    "Package " + product.getCode() + " has no released version in force"));
  }

  private ProductVersion renewal(RiskProduct product, Integer wanted, LocalDate today) {
    if (wanted != null) {
      ProductVersion v = versions.require(product.getCode(), wanted);
      if (v.getStatus() == ProductVersionStatus.RELEASED
          || v.getStatus() == ProductVersionStatus.SUPERSEDED) {
        return v;
      }
    }
    return versions
        .inForceEntity(product.getCode(), today)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    NOT_SELLABLE,
                    "Package " + product.getCode() + " expired and has no current version"));
  }

  private ProductVersion endorsement(
      RiskProduct product, Integer original, LocalDate ratingDate, LocalDate today) {
    if (original != null) {
      return versions.require(product.getCode(), original);
    }
    Optional<ProductVersion> inForce = versions.inForceEntity(product.getCode(), ratingDate);
    return inForce
        .or(() -> versions.inForceEntity(product.getCode(), today))
        .orElseGet(() -> versions.entities(product.getCode()).get(0));
  }

  /**
   * The resolved scheme.
   *
   * @param versionNo package version, null for a product without versions
   * @param rate scheme (or panel insurer) rate in percent, null when none
   * @param minimumPremium minimum premium
   * @param commissionRate default commission rate in percent
   * @param manualRateAllowed whether other item rates are accepted without an exception
   * @param overrideRef approved exception used, null when none
   * @param nonCurrent priced on a version other than the current one
   */
  public record Scheme(
      Integer versionNo,
      BigDecimal rate,
      BigDecimal minimumPremium,
      BigDecimal commissionRate,
      boolean manualRateAllowed,
      String overrideRef,
      boolean nonCurrent) {

    static Scheme of(RiskProduct p) {
      return new Scheme(
          null,
          p.getDefaultRate(),
          p.getMinimumPremium(),
          p.getDefaultCommissionRate(),
          true,
          null,
          false);
    }

    static Scheme of(ProductVersion v, String insurerCode, String overrideRef, boolean nonCurrent) {
      SchemeTerms terms = v.getScheme();
      VersionInsurer insurer = null;
      if (insurerCode != null && !v.getInsurers().isEmpty()) {
        insurer = v.insurer(insurerCode);
        if (insurer == null) {
          throw new BusinessRuleException(
              "INSURER_NOT_ON_PACKAGE",
              "Insurer "
                  + insurerCode
                  + " is not on the panel of "
                  + v.getProductCode()
                  + " version "
                  + v.getVersionNo());
        }
      }
      BigDecimal rate =
          insurer != null && insurer.rate() != null ? insurer.rate() : terms.defaultRate();
      BigDecimal minimum =
          insurer != null && insurer.minimumPremium() != null
              ? insurer.minimumPremium()
              : terms.minimumPremium();
      return new Scheme(
          v.getVersionNo(),
          rate,
          minimum,
          terms.defaultCommissionRate(),
          terms.manualRateAllowed(),
          overrideRef,
          nonCurrent);
    }

    /**
     * The item rates of a query that differ from the scheme rate and are not accepted as manual
     * rates.
     *
     * @param query query
     * @return deviating rates, distinct
     */
    public List<BigDecimal> deviatingRates(RatingQuery query) {
      if (versionNo == null || manualRateAllowed || rate == null) {
        return List.of();
      }
      return query.items().stream()
          .map(RatingQuery.Item::ratePercent)
          .filter(Objects::nonNull)
          .filter(r -> r.compareTo(rate) != 0)
          .distinct()
          .toList();
    }
  }
}
