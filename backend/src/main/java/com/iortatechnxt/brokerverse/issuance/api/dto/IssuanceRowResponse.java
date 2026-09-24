package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService.IssuanceRow;
import java.time.Instant;
import java.util.List;

/**
 * A row of the Issuance Workbench.
 *
 * @param accountId account
 * @param arn Account Reference Number
 * @param clientCode client code
 * @param clientName client
 * @param status account status
 * @param productCode product
 * @param lineCode product line
 * @param insurerCode insurer
 * @param mortgageeBank mortgagee bank
 * @param termYears policy years
 * @param policyNumbers policy numbers recorded
 * @param epolicyId e-policy of the row
 * @param epolicyStatus e-policy status
 * @param epolicyFile e-policy file
 * @param receivedAt e-policy received at
 */
public record IssuanceRowResponse(
    Long accountId,
    String arn,
    String clientCode,
    String clientName,
    String status,
    String productCode,
    String lineCode,
    String insurerCode,
    String mortgageeBank,
    int termYears,
    List<String> policyNumbers,
    Long epolicyId,
    String epolicyStatus,
    String epolicyFile,
    Instant receivedAt) {

  /**
   * Maps a row.
   *
   * @param row row
   * @return response
   */
  public static IssuanceRowResponse from(IssuanceRow row) {
    Account a = row.account();
    Epolicy e = row.epolicy();
    return new IssuanceRowResponse(
        a.getId(),
        a.getArn(),
        a.getClientCode(),
        a.getClientName(),
        a.getStatus().name(),
        a.getProductCode(),
        a.getLineCode(),
        a.getInsurerCode(),
        a.getMortgageeBank(),
        a.getTermYears(),
        a.getPolicyNumbers(),
        e == null ? null : e.getId(),
        e == null ? null : e.getStatus().name(),
        e == null ? null : e.getFileName(),
        e == null ? null : e.getCreatedAt());
  }
}
