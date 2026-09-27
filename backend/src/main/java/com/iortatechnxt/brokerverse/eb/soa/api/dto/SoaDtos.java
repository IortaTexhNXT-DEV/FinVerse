package com.iortatechnxt.brokerverse.eb.soa.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the SOA register (FR-EB-053). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class SoaDtos {

  private SoaDtos() {}

  /**
   * Invoices billed by an SOA.
   *
   * @param invoiceNos invoices, null to keep the linked ones
   */
  public record InvoicesRequest(List<String> invoiceNos) {}

  /**
   * A rejection.
   *
   * @param reasonCode reason (list EB_SOA_REJECT_REASON)
   * @param remarks remarks
   */
  public record RejectRequest(String reasonCode, String remarks) {}

  /**
   * An SOA.
   *
   * @param id id
   * @param soaNo intake number
   * @param programmeId programme
   * @param programmeNo programme number
   * @param clientName client
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param insurerSoaNo insurer's SOA number
   * @param periodFrom period from
   * @param periodTo period to
   * @param amount amount
   * @param currency currency
   * @param attachmentId the SOA file
   * @param status status
   * @param receivedOn received on
   * @param validatedBy validated by
   * @param releasedAt released at
   * @param rejectReason rejection reason
   * @param remarks remarks
   * @param invoices invoices billed with their payment status
   */
  public record SoaResponse(
      Long id,
      String soaNo,
      Long programmeId,
      String programmeNo,
      String clientName,
      String insurerCode,
      String insurerName,
      String insurerSoaNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal amount,
      String currency,
      Long attachmentId,
      String status,
      LocalDate receivedOn,
      String validatedBy,
      Instant releasedAt,
      String rejectReason,
      String remarks,
      List<InvoiceStatus> invoices) {

    /**
     * Maps an SOA.
     *
     * @param s SOA
     * @param programme programme, may be null
     * @param insurerName insurer name
     * @param payment payment status by invoice
     * @return response
     */
    public static SoaResponse from(
        EbSoa s, EbProgramme programme, String insurerName, Map<String, String> payment) {
      return new SoaResponse(
          s.getId(),
          s.getSoaNo(),
          s.getProgrammeId(),
          programme == null ? null : programme.getProgrammeNo(),
          programme == null ? null : programme.getClientName(),
          s.getInsurerCode(),
          insurerName,
          s.getInsurerSoaNo(),
          s.getPeriodFrom(),
          s.getPeriodTo(),
          s.getAmount(),
          s.getCurrency(),
          s.getAttachmentId(),
          s.getStatus().name(),
          s.getReceivedOn(),
          s.getValidatedBy(),
          s.getReleasedAt(),
          s.getRejectReason(),
          s.getRemarks(),
          s.getInvoiceNos().stream()
              .map(n -> new InvoiceStatus(n, payment.getOrDefault(n, "UNKNOWN")))
              .toList());
    }
  }

  /**
   * An invoice with its payment status.
   *
   * @param invoiceNo invoice
   * @param paymentStatus ledger payment status
   */
  public record InvoiceStatus(String invoiceNo, String paymentStatus) {}

  /**
   * A booked invoice of a programme.
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param insurerCode insurer
   * @param bookingDate booked on
   * @param grossPremium gross premium
   * @param currency currency
   * @param paymentStatus payment status
   */
  public record ProgrammeInvoice(
      String invoiceNo,
      String arn,
      String insurerCode,
      LocalDate bookingDate,
      BigDecimal grossPremium,
      String currency,
      String paymentStatus) {

    /**
     * Maps an invoice.
     *
     * @param i invoice
     * @return response
     */
    public static ProgrammeInvoice from(OpsInvoice i) {
      return new ProgrammeInvoice(
          i.getInvoiceNo(),
          i.getArn(),
          i.getInsurerCode(),
          i.getBookingDate(),
          i.getGrossPremium(),
          i.getCurrency(),
          i.getPaymentStatus().name());
    }
  }
}
