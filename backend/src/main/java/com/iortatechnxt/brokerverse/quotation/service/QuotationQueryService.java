package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.PackageDates;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationVersion;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quotation reads for screens and other modules: one quotation with its current content, look-up by
 * ARN (contract for Operations Cashiering, with the direct-payment flag), the list, the versions
 * and the diff between two versions (BRNB.020).
 */
@Service
@Transactional(readOnly = true)
public class QuotationQueryService {

  /** Parameter: days before the end of validity from which a quotation is expiring. */
  public static final String EXPIRING_DAYS = "QUOTATION_EXPIRING_DAYS";

  private static final int DEFAULT_EXPIRING = 7;

  private final QuotationRepository quotations;
  private final QuotationVersions versions;
  private final SystemParameterService parameters;
  private final ProductVersionQueryService packageVersions;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param quotations quotations
   * @param versions version store
   * @param parameters business parameters
   * @param packageVersions package versions (the package term of a quotation without dates)
   * @param clock clock
   */
  public QuotationQueryService(
      QuotationRepository quotations,
      QuotationVersions versions,
      SystemParameterService parameters,
      ProductVersionQueryService packageVersions,
      Clock clock) {
    this.quotations = quotations;
    this.versions = versions;
    this.parameters = parameters;
    this.packageVersions = packageVersions;
    this.clock = clock;
  }

  /**
   * The cover period of a quotation as users read it (header and documents): inception to expiry
   * when entered, else the term of the package version that priced it (or of the package in force);
   * empty only when neither is known.
   *
   * @param q quotation
   * @param c its content
   * @return text, empty when unknown
   */
  public String coverPeriod(Quotation q, QuotationContent c) {
    if (c.periodFrom() != null || c.periodTo() != null) {
      return DisplayFormat.period(c.periodFrom(), c.periodTo());
    }
    Optional<ProductVersionView> version =
        c.schemeVersion() == null
            ? packageVersions.current(q.getProductCode())
            : packageVersions.version(q.getProductCode(), c.schemeVersion());
    return version
        .map(ProductVersionView::dates)
        .map(QuotationQueryService::packageTerm)
        .orElse("");
  }

  private static String packageTerm(PackageDates d) {
    LocalDate start = d.packageStartDate() == null ? d.effectiveFrom() : d.packageStartDate();
    String term = DisplayFormat.period(start, d.packageEndDate());
    return term.isEmpty() ? "" : "Package term " + term;
  }

  /**
   * Last validity date of the quotations listed as expiring (parameter QUOTATION_EXPIRING_DAYS).
   *
   * @return today plus the expiring window
   */
  public LocalDate expiringLimit() {
    return BusinessClock.today(clock)
        .plusDays(parameters.intValue(EXPIRING_DAYS, DEFAULT_EXPIRING));
  }

  /**
   * One quotation with its account references loaded.
   *
   * @param id id
   * @return quotation
   */
  public Quotation get(Long id) {
    Quotation q =
        quotations
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(QuotationService.ENTITY, id));
    Hibernate.initialize(q.getAccountArns());
    return q;
  }

  /**
   * The quotation of an ARN (contract for Operations Cashiering: the direct-payment flag decides
   * whether BDOI collects the premium, MKTID.011). An account ARN with a suffix (-01, -02...) finds
   * the quotation it came from.
   *
   * @param arn Account Reference Number
   * @return summary
   */
  public QuotationSummary getByArn(String arn) {
    String key = arn == null ? "" : arn.strip();
    Quotation q =
        quotations
            .findByArn(key)
            .or(() -> quotations.findByArn(key.replaceFirst("-\\d{2}$", "")))
            .orElseThrow(() -> new ResourceNotFoundException(QuotationService.ENTITY, key));
    return QuotationSummary.of(q);
  }

  /**
   * The quotation of a renewal on the New Business path (Renewal status tracking, BRRN.033).
   *
   * @param renewalRef renewal reference
   * @return the quotation, empty when the renewal has none
   */
  public Optional<Quotation> getByRenewalRef(String renewalRef) {
    Optional<Quotation> q = quotations.findByRenewalRef(renewalRef);
    q.ifPresent(x -> Hibernate.initialize(x.getAccountArns()));
    return q;
  }

  /**
   * Quotations matching the criteria.
   *
   * @param search criteria
   * @param pageable page and sort
   * @return page
   */
  public Page<Quotation> search(QuotationSearch search, Pageable pageable) {
    return quotations.findAll(search.toSpecification(), pageable);
  }

  /**
   * Quotations of a client, newest first.
   *
   * @param clientId client
   * @return quotations
   */
  public List<Quotation> byClient(Long clientId) {
    return quotations.findByClientIdOrderByCreatedAtDesc(clientId);
  }

  /**
   * Content of the current version.
   *
   * @param q quotation
   * @return content
   */
  public QuotationContent content(Quotation q) {
    return versions.current(q);
  }

  /**
   * Content of one version.
   *
   * @param id quotation
   * @param versionNo version number
   * @return content
   */
  public QuotationContent content(Long id, int versionNo) {
    return versions.content(get(id).getId(), versionNo);
  }

  /**
   * Every version of a quotation, oldest first.
   *
   * @param id quotation
   * @return versions
   */
  public List<QuotationVersion> versions(Long id) {
    return versions.all(get(id).getId());
  }

  /**
   * Differences between two versions.
   *
   * @param id quotation
   * @param from older version
   * @param to newer version
   * @return differences
   */
  public QuotationDiff diff(Long id, int from, int to) {
    return QuotationDiff.between(from, content(id, from), to, content(id, to));
  }
}
