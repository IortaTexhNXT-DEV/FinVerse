package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * A coverage / peril of a package version (PMADD01; table {@code cat_package_coverage}).
 *
 * @param coverageCode coverage code of the product line ({@code cat_coverage})
 * @param included included in the package
 * @param optional optional (the client may choose it)
 * @param limitAmount limit, null when none
 * @param subLimit sub-limit, null when none
 * @param deductibleAmount deductible amount, null when none
 * @param deductiblePercent deductible percent, null when none
 * @param deductibleText deductible wording, null when none
 * @param sortOrder display order
 */
@Embeddable
public record VersionCoverage(
    @Column(name = "coverage_code", nullable = false, length = 30) String coverageCode,
    @Column(nullable = false) boolean included,
    @Column(nullable = false) boolean optional,
    @Column(name = "limit_amount", precision = 19, scale = 2) BigDecimal limitAmount,
    @Column(name = "sub_limit", precision = 19, scale = 2) BigDecimal subLimit,
    @Column(name = "deductible_amount", precision = 19, scale = 2) BigDecimal deductibleAmount,
    @Column(name = "deductible_percent", precision = 19, scale = 8) BigDecimal deductiblePercent,
    @Column(name = "deductible_text", length = 500) String deductibleText,
    @Column(name = "sort_order", nullable = false) int sortOrder) {}
