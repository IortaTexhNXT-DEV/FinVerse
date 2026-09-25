package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * The terms of one insurer on one coverage of a package version (PMADD02; table {@code
 * cat_package_insurer_term}).
 *
 * @param insurerCode insurer party code
 * @param coverageCode coverage code
 * @param included covered by this insurer
 * @param limitAmount limit, null when none
 * @param subLimit sub-limit, null when none
 * @param deductibleAmount deductible amount, null when none
 * @param deductiblePercent deductible percent, null when none
 * @param deductibleText deductible wording, null when none
 * @param clauseCodes comma-separated clause codes, null when none
 * @param remarks remarks
 */
@Embeddable
public record VersionInsurerTerm(
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Column(name = "coverage_code", nullable = false, length = 30) String coverageCode,
    @Column(nullable = false) boolean included,
    @Column(name = "limit_amount", precision = 19, scale = 2) BigDecimal limitAmount,
    @Column(name = "sub_limit", precision = 19, scale = 2) BigDecimal subLimit,
    @Column(name = "deductible_amount", precision = 19, scale = 2) BigDecimal deductibleAmount,
    @Column(name = "deductible_percent", precision = 19, scale = 8) BigDecimal deductiblePercent,
    @Column(name = "deductible_text", length = 500) String deductibleText,
    @Column(name = "clause_codes", length = 1000) String clauseCodes,
    @Column(length = 1000) String remarks) {

  /**
   * The clause codes as a list.
   *
   * @return codes, empty when none
   */
  public List<String> clauseCodeList() {
    return clauseCodes == null || clauseCodes.isBlank()
        ? List.of()
        : Arrays.stream(clauseCodes.split(",")).map(String::strip).toList();
  }
}
