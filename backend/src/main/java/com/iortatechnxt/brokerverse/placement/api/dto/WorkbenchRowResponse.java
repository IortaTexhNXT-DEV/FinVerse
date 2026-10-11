package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService.WorkbenchRow;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A row of the Placement Workbench: client, proposal (ARN), status, product line and department,
 * with the current slip and hold cover.
 *
 * @param accountId account id
 * @param arn Account Reference Number (proposal number)
 * @param clientId client id
 * @param clientCode client code
 * @param clientName client name
 * @param status account status
 * @param productCode product
 * @param lineCode product line
 * @param department sales department
 * @param marketSegment market segment
 * @param insurerCode insurer
 * @param insurerBranch insurer branch
 * @param grossPremium gross premium
 * @param directPayment paid directly to the insurer
 * @param paymentStatus payment gate position
 * @param slipId current slip id
 * @param slipNo current slip reference
 * @param slipStatus current slip status
 * @param holdCoverStatus latest hold cover status
 * @param holdCoverExpiry latest hold cover expiry
 */
public record WorkbenchRowResponse(
    Long accountId,
    String arn,
    Long clientId,
    String clientCode,
    String clientName,
    String status,
    String productCode,
    String lineCode,
    String department,
    String marketSegment,
    String insurerCode,
    String insurerBranch,
    BigDecimal grossPremium,
    boolean directPayment,
    String paymentStatus,
    Long slipId,
    String slipNo,
    String slipStatus,
    String holdCoverStatus,
    LocalDate holdCoverExpiry) {

  /**
   * Maps a workbench row.
   *
   * @param row row
   * @return response
   */
  public static WorkbenchRowResponse from(WorkbenchRow row) {
    Account a = row.account();
    PlacementSlip slip = row.slip();
    HoldCover cover = row.holdCover();
    return new WorkbenchRowResponse(
        a.getId(),
        a.getArn(),
        a.getClientId(),
        a.getClientCode(),
        a.getClientName(),
        a.getStatus().name(),
        a.getProductCode(),
        a.getLineCode(),
        a.getSales().department(),
        a.getMarketSegment(),
        a.getInsurerCode(),
        a.getInsurerBranch(),
        a.getPremium().grossPremium(),
        a.isDirectPayment(),
        a.getLifecycle().getPaymentStatus().name(),
        slip == null ? null : slip.getId(),
        slip == null ? null : slip.displayNo(),
        slip == null ? null : slip.getStatus().name(),
        cover == null ? null : cover.getStatus().name(),
        cover == null ? null : cover.getExpiryDate());
  }
}
