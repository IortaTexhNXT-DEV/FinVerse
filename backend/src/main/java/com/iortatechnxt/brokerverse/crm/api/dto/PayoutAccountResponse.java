package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccount;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import java.time.Instant;

/**
 * CA / SA information of a client (MKT 2.25.0).
 *
 * @param id id
 * @param mode credit to account or check
 * @param payeeName account or check payee name
 * @param accountNo BDO account number (credit to account)
 * @param sourceModule capturing module
 * @param sourceRef its reference (refund request)
 * @param active live row
 * @param createdAt captured at
 * @param createdBy captured by
 */
public record PayoutAccountResponse(
    Long id,
    PayoutMode mode,
    String payeeName,
    String accountNo,
    String sourceModule,
    String sourceRef,
    boolean active,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps an account.
   *
   * @param a account
   * @return response
   */
  public static PayoutAccountResponse from(ClientPayoutAccount a) {
    return new PayoutAccountResponse(
        a.getId(),
        a.getMode(),
        a.getPayeeName(),
        a.getAccountNo(),
        a.getSourceModule(),
        a.getSourceRef(),
        a.isActive(),
        a.getCreatedAt(),
        a.getCreatedBy());
  }
}
