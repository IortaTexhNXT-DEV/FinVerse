package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.Lpo;
import com.iortatechnxt.brokerverse.claims.domain.LpoCover;
import com.iortatechnxt.brokerverse.claims.domain.LpoStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A local purchase order.
 *
 * @param id id
 * @param claimId claim
 * @param claimNo claim number
 * @param lpoNo LPO number
 * @param garageCode garage code
 * @param garageName garage name
 * @param coverType OD or TP
 * @param issueDate issue date
 * @param grossAmount gross
 * @param discountAmount discount
 * @param netAmount net = gross − discount
 * @param currency claim currency
 * @param description description
 * @param status status
 * @param cancelReason cancellation reason
 */
public record LpoResponse(
    Long id,
    Long claimId,
    String claimNo,
    String lpoNo,
    String garageCode,
    String garageName,
    LpoCover coverType,
    LocalDate issueDate,
    BigDecimal grossAmount,
    BigDecimal discountAmount,
    BigDecimal netAmount,
    String currency,
    String description,
    LpoStatus status,
    String cancelReason) {

  /**
   * Maps an LPO (claim and garage loaded).
   *
   * @param l LPO
   * @return response
   */
  public static LpoResponse from(Lpo l) {
    return new LpoResponse(
        l.getId(),
        l.getClaim().getId(),
        l.getClaim().getClaimNo(),
        l.getLpoNo(),
        l.getGarage().getCode(),
        l.getGarage().getName(),
        l.getCoverType(),
        l.getIssueDate(),
        l.getGrossAmount(),
        l.getDiscountAmount(),
        l.getNetAmount(),
        l.getClaim().getCurrency(),
        l.getDescription(),
        l.getStatus(),
        l.getCancelReason());
  }
}
