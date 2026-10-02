package com.iortatechnxt.brokerverse.reserves.domain;

import java.math.BigDecimal;

/**
 * Maintainable values of a reserve parameter set. Percentages are stated as numbers of percent
 * (12.5 = 12.5 %).
 *
 * @param ibnrMethod IBNR method
 * @param ibnrRate IBNR rate % of earned premium (rate method)
 * @param triangleBasis paid or incurred triangle (chain-ladder)
 * @param developmentPeriod year or quarter development (chain-ladder)
 * @param accidentPeriods number of accident periods in the triangle (chain-ladder)
 * @param mfadPct margin for adverse deviation % of OSLR + IBNR
 * @param ulaePct ULAE provision % of OSLR + IBNR
 * @param expectedLossRatio expected loss ratio % (liability adequacy test)
 * @param treatyCommissionPct reinsurance commission % on treaty premium (UCR)
 * @param facCommissionPct reinsurance commission % on facultative premium (UCR)
 * @param remarks free text
 */
public record ReserveParameterTerms(
    IbnrMethod ibnrMethod,
    BigDecimal ibnrRate,
    TriangleBasis triangleBasis,
    DevelopmentPeriod developmentPeriod,
    int accidentPeriods,
    BigDecimal mfadPct,
    BigDecimal ulaePct,
    BigDecimal expectedLossRatio,
    BigDecimal treatyCommissionPct,
    BigDecimal facCommissionPct,
    String remarks) {}
