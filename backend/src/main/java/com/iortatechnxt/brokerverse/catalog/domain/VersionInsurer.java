package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * An insurer of a package version (PMADD02, PQ03; table {@code cat_package_insurer}).
 *
 * @param companyId company of the insurer record
 * @param insurerCode insurer party code
 * @param role LEAD, PARTICIPANT or PANEL
 * @param sharePercent co-insurance share, null for a panel insurer
 * @param rate insurer premium rate in percent, null for the scheme rate
 * @param minimumPremium insurer minimum premium, null for the scheme minimum
 * @param defaultBranchCode default insurer branch, null when none
 */
@Embeddable
public record VersionInsurer(
    @Column(name = "company_id", nullable = false) Long companyId,
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) PackageInsurerRole role,
    @Column(name = "share_percent", precision = 19, scale = 8) BigDecimal sharePercent,
    @Column(precision = 19, scale = 8) BigDecimal rate,
    @Column(name = "minimum_premium", precision = 19, scale = 2) BigDecimal minimumPremium,
    @Column(name = "default_branch_code", length = 20) String defaultBranchCode) {}
