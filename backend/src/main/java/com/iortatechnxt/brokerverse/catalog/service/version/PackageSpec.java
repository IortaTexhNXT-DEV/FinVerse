package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The content of a package version as negotiated and signed off (BRPM.015, PMADD01/02; design
 * sections 4.2 and 9.1): what {@link PackageSetupService} turns into a DRAFT catalog version. Built
 * by {@code productmaint} from the request's proposed terms (MBS set-up) and by the catalog "New
 * Version" editor. Rates are percentages (12.5 = 12.5 %), amounts are in the product currency with
 * scale 2.
 *
 * @param companyId company of the request (insurer look-ups)
 * @param productCode existing risk code, or the code of a new product (checked against the line's
 *     code pattern, {@code PRODUCT_CODE_PATTERN})
 * @param newProduct identity of a product that does not exist yet (NEW requests), else null
 * @param baseVersionNo version the draft starts from (AMEND / UPDATE / RENEW / REACTIVATE), null
 *     for a new product
 * @param rateScheme package rate scheme
 * @param dates effectivity and package term
 * @param coverages coverages / perils of the package (at least one basic coverage before submit)
 * @param insurers insurers of the package (panel, lead, participants)
 * @param insurerTerms insurer x coverage terms (one per panel insurer and included coverage before
 *     submit)
 * @param origin where the version comes from (request, ManCom sign-off)
 */
public record PackageSpec(
    Long companyId,
    String productCode,
    NewProduct newProduct,
    Integer baseVersionNo,
    RateScheme rateScheme,
    PackageDates dates,
    List<Coverage> coverages,
    List<Insurer> insurers,
    List<InsurerTerm> insurerTerms,
    Origin origin) {

  /** Defensive copies. */
  public PackageSpec {
    coverages = coverages == null ? List.of() : List.copyOf(coverages);
    insurers = insurers == null ? List.of() : List.copyOf(insurers);
    insurerTerms = insurerTerms == null ? List.of() : List.copyOf(insurerTerms);
  }

  /**
   * Identity of a new packaged product (PMADD01: line and cover type are mandatory).
   *
   * @param name product name
   * @param lineCode product line (LOB)
   * @param coverTypeCode cover type or subtype
   * @param marketSegments market segments (LOV MARKET_SEGMENT)
   * @param clientCode client of a client-specific package (PQ14), null for a generic programme
   */
  public record NewProduct(
      String name,
      String lineCode,
      String coverTypeCode,
      List<String> marketSegments,
      String clientCode) {

    /** Defensive copy. */
    public NewProduct {
      marketSegments = marketSegments == null ? List.of() : List.copyOf(marketSegments);
    }
  }

  /**
   * The package rate scheme (BRPM.007); on release it is projected on the product's commercial
   * columns.
   *
   * @param defaultRate default premium rate in percent
   * @param minimumPremium minimum premium
   * @param defaultCommissionRate default commission rate in percent
   * @param maxSumInsured package TSI limit (TSU routing)
   * @param ratingBasisNote computation basis agreed with the insurers (free text, PQ12)
   */
  public record RateScheme(
      BigDecimal defaultRate,
      BigDecimal minimumPremium,
      BigDecimal defaultCommissionRate,
      BigDecimal maxSumInsured,
      String ratingBasisNote) {}

  /**
   * Effectivity and term of the package (BRPM.006/017).
   *
   * @param effectiveFrom date the version sells from once released
   * @param packageStartDate start of the insurer agreement
   * @param packageEndDate end of the insurer agreement (expiry monitor)
   * @param anniversaryDate anniversary date, null when none (PQ11)
   */
  public record PackageDates(
      LocalDate effectiveFrom,
      LocalDate packageStartDate,
      LocalDate packageEndDate,
      LocalDate anniversaryDate) {}

  /**
   * A deductible, as an amount, a percentage and / or wording.
   *
   * @param amount deductible amount, null when none
   * @param percent deductible percent, null when none
   * @param text deductible wording, null when none
   */
  public record Deductible(BigDecimal amount, BigDecimal percent, String text) {}

  /**
   * A coverage / peril of the package (PMADD01).
   *
   * @param coverageCode coverage code ({@code cat_coverage})
   * @param included included in the package
   * @param optional optional (client may choose)
   * @param limitAmount limit, null when none
   * @param subLimit sub-limit, null when none
   * @param deductible deductible, null when none
   * @param sortOrder display order
   */
  public record Coverage(
      String coverageCode,
      boolean included,
      boolean optional,
      BigDecimal limitAmount,
      BigDecimal subLimit,
      Deductible deductible,
      int sortOrder) {}

  /**
   * An insurer of the package (PMADD02, PQ03).
   *
   * @param insurerCode insurer party code ({@code cat_insurer})
   * @param role LEAD, PARTICIPANT or PANEL
   * @param sharePercent co-insurance share, null for a panel insurer (shares sum to 100 when used)
   * @param rate insurer-specific premium rate in percent, null for the scheme rate
   * @param minimumPremium insurer-specific minimum premium, null for the scheme minimum
   * @param defaultBranchCode default insurer branch (LGT), null when none
   */
  public record Insurer(
      String insurerCode,
      PackageInsurerRole role,
      BigDecimal sharePercent,
      BigDecimal rate,
      BigDecimal minimumPremium,
      String defaultBranchCode) {}

  /**
   * The terms of one insurer on one coverage (PMADD02).
   *
   * @param insurerCode insurer party code
   * @param coverageCode coverage code
   * @param included covered by this insurer
   * @param limitAmount limit, null when none
   * @param subLimit sub-limit, null when none
   * @param deductible deductible, null when none
   * @param clauseCodes warranties, clauses and exclusions ({@code cat_clause} codes)
   * @param remarks remarks
   */
  public record InsurerTerm(
      String insurerCode,
      String coverageCode,
      boolean included,
      BigDecimal limitAmount,
      BigDecimal subLimit,
      Deductible deductible,
      List<String> clauseCodes,
      String remarks) {

    /** Defensive copy. */
    public InsurerTerm {
      clauseCodes = clauseCodes == null ? List.of() : List.copyOf(clauseCodes);
    }
  }

  /**
   * Where a version comes from (BRPM.015, audit).
   *
   * @param sourceRequestNo package request number (PKR-...), null for a catalog-only change
   * @param mancomSignoffRef ManCom sign-off reference, null when none
   * @param changeSummary what changes against the base version
   */
  public record Origin(String sourceRequestNo, String mancomSignoffRef, String changeSummary) {}
}
