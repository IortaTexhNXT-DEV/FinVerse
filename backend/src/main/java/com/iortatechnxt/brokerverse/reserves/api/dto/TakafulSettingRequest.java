package com.iortatechnxt.brokerverse.reserves.api.dto;

import com.iortatechnxt.brokerverse.reserves.domain.TakafulTerms;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Takaful surplus settings of a company.
 *
 * @param companyId company
 * @param enabled whether valuation runs compute the surplus
 * @param productCodes comma separated takaful product codes (blank = every product)
 * @param participantSharePct participants' share % of the surplus
 * @param taxPct tax % on the participants' share
 * @param costCenter cost centre of the surplus expense journal
 */
public record TakafulSettingRequest(
    @NotNull Long companyId,
    boolean enabled,
    @Size(max = 500) @Pattern(regexp = "[A-Za-z0-9,\\- ]*") String productCodes,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal participantSharePct,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxPct,
    @Size(max = 20) String costCenter) {

  /**
   * Domain values.
   *
   * @return terms
   */
  public TakafulTerms toTerms() {
    return new TakafulTerms(enabled, productCodes, participantSharePct, taxPct, costCenter);
  }
}
