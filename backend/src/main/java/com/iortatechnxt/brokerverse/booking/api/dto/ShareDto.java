package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * An insurer's share of an invoice.
 *
 * @param insurerCode insurer party code
 * @param sharePct share in percent
 */
public record ShareDto(
    @NotBlank @Size(max = 30) String insurerCode,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal sharePct) {

  /**
   * Maps a share.
   *
   * @param s share
   * @return DTO
   */
  public static ShareDto from(InsurerShare s) {
    return new ShareDto(s.insurerCode(), s.sharePct());
  }

  /**
   * To the domain value.
   *
   * @return share
   */
  public InsurerShare toShare() {
    return new InsurerShare(insurerCode, sharePct);
  }
}
