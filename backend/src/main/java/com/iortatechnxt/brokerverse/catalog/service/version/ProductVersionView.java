package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model of one package product version (BRPM.006/007/017): its status, dates, rate scheme,
 * content and checkpoint. Returned by {@link ProductVersionQueryService}; {@code productmaint} uses
 * it to pre-fill RENEW / AMEND requests and to list expiring packages.
 *
 * @param productCode risk code
 * @param productName product name
 * @param versionNo version number
 * @param status version status
 * @param dates effectivity and package term
 * @param effectiveTo last day the version sells (set when superseded), null while current
 * @param rateScheme rate scheme
 * @param coverages coverages / perils
 * @param insurers insurers
 * @param insurerTerms insurer x coverage terms
 * @param origin source request, ManCom reference and change summary
 * @param checkpoint validation checkpoint (PMADD06)
 */
public record ProductVersionView(
    String productCode,
    String productName,
    int versionNo,
    ProductVersionStatus status,
    PackageSpec.PackageDates dates,
    LocalDate effectiveTo,
    PackageSpec.RateScheme rateScheme,
    List<PackageSpec.Coverage> coverages,
    List<PackageSpec.Insurer> insurers,
    List<PackageSpec.InsurerTerm> insurerTerms,
    PackageSpec.Origin origin,
    Checkpoint checkpoint) {

  /** Defensive copies. */
  public ProductVersionView {
    coverages = coverages == null ? List.of() : List.copyOf(coverages);
    insurers = insurers == null ? List.of() : List.copyOf(insurers);
    insurerTerms = insurerTerms == null ? List.of() : List.copyOf(insurerTerms);
  }

  /**
   * The version reference.
   *
   * @return product code, version number, status and effective date
   */
  public VersionRef ref() {
    return new VersionRef(
        productCode, versionNo, status, dates == null ? null : dates.effectiveFrom());
  }

  /**
   * Who submitted, validated or returned the version (PMADD06; all null while DRAFT).
   *
   * @param submittedBy maker who submitted it for validation
   * @param submittedAt submission time
   * @param validatedBy validator (never the maker or submitter)
   * @param validatedAt validation time
   * @param returnedReason reason of the last return to DRAFT, null when none
   */
  public record Checkpoint(
      String submittedBy,
      Instant submittedAt,
      String validatedBy,
      Instant validatedAt,
      String returnedReason) {}
}
