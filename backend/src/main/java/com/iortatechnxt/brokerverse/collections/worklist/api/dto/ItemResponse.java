package com.iortatechnxt.brokerverse.collections.worklist.api.dto;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Classification;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Figures;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Parties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A collection item of the worklist (BRCLXN.001-012).
 *
 * @param id id
 * @param invoiceNo invoice
 * @param arn account
 * @param policyNo policy
 * @param policyYear policy year
 * @param clientCode client
 * @param assuredName assured
 * @param insurerCode insurer
 * @param segment market segment
 * @param salesUnit sales unit
 * @param unitHead Unit Head
 * @param aoUsername account officer
 * @param currency currency
 * @param bookingDate booking date
 * @param inceptionDate inception
 * @param expiryDate expiry
 * @param dpFlag direct payment
 * @param cwtFlag 2% CWT
 * @param invoiceCategory REGULAR or DIRECT_BILL
 * @param grossPremium gross premium
 * @param netOutstanding outstanding premium receivable
 * @param outstandingPr2307 PR2307 still open
 * @param agingDays age in days
 * @param agingBracket bracket
 * @param paymentStatus ledger payment status
 * @param status item status
 * @param listedOn listed on
 * @param completedOn completed on
 * @param currentHandler handler
 * @param dispositionCode current disposition
 * @param category tagging category
 * @param taggingOwner tagging owner
 * @param lastEffortAt last effort
 * @param lastEffortCode last effort code
 * @param remarks remarks
 * @param promiseStatus promise flag
 * @param escalationLevel escalation flag
 * @param installmentOverdue overdue installment flag
 * @param lastRefreshedAt last refresh
 */
public record ItemResponse(
    Long id,
    String invoiceNo,
    String arn,
    String policyNo,
    int policyYear,
    String clientCode,
    String assuredName,
    String insurerCode,
    String segment,
    String salesUnit,
    String unitHead,
    String aoUsername,
    String currency,
    LocalDate bookingDate,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    boolean dpFlag,
    boolean cwtFlag,
    String invoiceCategory,
    BigDecimal grossPremium,
    BigDecimal netOutstanding,
    BigDecimal outstandingPr2307,
    int agingDays,
    String agingBracket,
    String paymentStatus,
    String status,
    LocalDate listedOn,
    LocalDate completedOn,
    String currentHandler,
    String dispositionCode,
    String category,
    String taggingOwner,
    Instant lastEffortAt,
    String lastEffortCode,
    String remarks,
    String promiseStatus,
    String escalationLevel,
    boolean installmentOverdue,
    Instant lastRefreshedAt) {

  /**
   * Maps an item.
   *
   * @param i item
   * @return response
   */
  public static ItemResponse from(CollectionItem i) {
    Parties p = i.getParties();
    Classification c = i.getClassification();
    Figures f = i.getFigures();
    return new ItemResponse(
        i.getId(),
        i.getInvoiceNo(),
        p.arn(),
        p.policyNo(),
        p.policyYear(),
        p.clientCode(),
        p.assuredName(),
        p.insurerCode(),
        c.segment(),
        c.salesUnit(),
        c.unitHeadUsername(),
        c.aoUsername(),
        c.currency(),
        c.bookingDate(),
        c.inceptionDate(),
        c.expiryDate(),
        c.dpFlag(),
        c.cwtFlag(),
        c.invoiceCategory().name(),
        f.grossPremium(),
        f.netOutstanding(),
        f.outstandingPr2307(),
        f.agingDays(),
        f.agingBracket(),
        f.paymentStatus().name(),
        i.getStatus().name(),
        i.getListedOn(),
        i.getCompletedOn(),
        i.getCurrentHandler(),
        i.getDispositionCode(),
        i.getCategory(),
        i.getTaggingOwner() == null ? null : i.getTaggingOwner().name(),
        i.getLastEffortAt(),
        i.getLastEffortCode(),
        i.getRemarks(),
        i.getPromiseStatus(),
        i.getEscalationLevel(),
        i.isInstallmentOverdue(),
        i.getLastRefreshedAt());
  }
}
