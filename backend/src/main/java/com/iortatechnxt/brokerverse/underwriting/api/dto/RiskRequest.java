package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.underwriting.domain.MarineDetails;
import com.iortatechnxt.brokerverse.underwriting.domain.RiskValues;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One risk (section) of a policy or certificate, at 100 %. When {@code premium} is omitted it is
 * computed as sum insured × rate %.
 *
 * @param description description
 * @param sumInsured sum insured
 * @param rate premium rate %
 * @param premium gross premium (optional)
 * @param occupation occupation / usage
 * @param accumulationZone accumulation zone
 * @param vesselName vessel (marine)
 * @param voyageFrom port of loading (marine)
 * @param voyageTo port of discharge (marine)
 * @param sailDate sailing date (marine)
 * @param blNo bill of lading no. (marine)
 * @param blDate bill of lading date (marine)
 * @param lcNo letter of credit no. (marine)
 * @param bankName LC bank (marine)
 * @param valuationBasis basis of valuation (marine)
 */
public record RiskRequest(
    @NotBlank @Size(max = 300) String description,
    @NotNull @Positive BigDecimal sumInsured,
    @DecimalMin("0") BigDecimal rate,
    @DecimalMin("0") BigDecimal premium,
    @Size(max = 120) String occupation,
    @Size(max = 40) String accumulationZone,
    @Size(max = 120) String vesselName,
    @Size(max = 80) String voyageFrom,
    @Size(max = 80) String voyageTo,
    LocalDate sailDate,
    @Size(max = 40) String blNo,
    LocalDate blDate,
    @Size(max = 40) String lcNo,
    @Size(max = 120) String bankName,
    @Size(max = 120) String valuationBasis) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATE_SCALE = 8;

  /**
   * Domain values (premium defaulted from the rate when omitted).
   *
   * @return values
   */
  public RiskValues toValues() {
    BigDecimal r = Money.nz(rate);
    BigDecimal p =
        premium != null
            ? Money.round(premium)
            : Money.round(
                sumInsured.multiply(r).divide(HUNDRED, RATE_SCALE, RoundingMode.HALF_EVEN));
    boolean marine = vesselName != null || lcNo != null || blNo != null;
    return new RiskValues(
        description,
        Money.round(sumInsured),
        r,
        p,
        blankToNull(occupation),
        blankToNull(accumulationZone),
        marine
            ? new MarineDetails(
                vesselName,
                voyageFrom,
                voyageTo,
                sailDate,
                blNo,
                blDate,
                lcNo,
                bankName,
                valuationBasis)
            : null);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
