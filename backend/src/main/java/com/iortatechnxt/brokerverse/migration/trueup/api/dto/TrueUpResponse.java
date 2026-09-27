package com.iortatechnxt.brokerverse.migration.trueup.api.dto;

import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An opening-balance adjustment (FY2027 true-up) with its evidence.
 *
 * @param reference MIG-TU-n
 * @param trueupNo 1, 2, 3 or F
 * @param asOf as-of date of the legacy trial balance
 * @param status PREPARED, FOR_APPROVAL, APPROVED, POSTED, RECONCILED or SIGNED
 * @param batchNo G03 batch
 * @param tbBatchNo G01 batch of the legacy trial balance
 * @param journalsPosted journals posted
 * @param itemsAdjusted open items adjusted
 * @param preparedBy preparer
 * @param preparedAt prepared at
 * @param approvedBy approver
 * @param approvedAt approved at
 * @param postedAt posted at
 * @param signedBy signer
 * @param signedAt signed at
 * @param remarks remarks
 */
public record TrueUpResponse(
    String reference,
    String trueupNo,
    LocalDate asOf,
    String status,
    String batchNo,
    String tbBatchNo,
    int journalsPosted,
    int itemsAdjusted,
    String preparedBy,
    Instant preparedAt,
    String approvedBy,
    Instant approvedAt,
    Instant postedAt,
    String signedBy,
    Instant signedAt,
    String remarks) {

  /**
   * Maps a true-up.
   *
   * @param t true-up
   * @param batchNo its G03 batch number
   * @param tbBatchNo its G01 batch number
   * @return response
   */
  public static TrueUpResponse from(MigTrueup t, String batchNo, String tbBatchNo) {
    return new TrueUpResponse(
        t.getReference(),
        t.getTrueupNo(),
        t.getAsOf(),
        t.getStatus().name(),
        batchNo,
        tbBatchNo,
        t.getJournalsPosted(),
        t.getItemsAdjusted(),
        t.getPreparedBy(),
        t.getPreparedAt(),
        t.getApprovedBy(),
        t.getApprovedAt(),
        t.getPostedAt(),
        t.getSignedBy(),
        t.getSignedAt(),
        t.getRemarks());
  }
}
