package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.DispatchStatus;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A service invoice or credit.
 *
 * @param id id
 * @param siNo number
 * @param typeCode type
 * @param kind invoice or credit
 * @param invoiceNo booked invoice
 * @param arn account
 * @param recipientCode recipient
 * @param recipientName recipient name
 * @param recipientEmail billing e-mail
 * @param issueDate issue date
 * @param currency currency
 * @param commission commission
 * @param vatOnCommission VAT on commission
 * @param wtaxAmount withholding tax
 * @param netAmount net amount
 * @param templateCode template
 * @param templateVersion template version
 * @param ownerUsername owning user
 * @param ownerPermission owning team
 * @param dispatchStatus dispatch status
 * @param dispatchError failure reason
 * @param creditOf service invoice credited
 * @param remarks remarks
 * @param createdBy user
 * @param createdAt time
 */
public record ServiceInvoiceResponse(
    Long id,
    String siNo,
    String typeCode,
    SiKind kind,
    String invoiceNo,
    String arn,
    String recipientCode,
    String recipientName,
    String recipientEmail,
    LocalDate issueDate,
    String currency,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    BigDecimal wtaxAmount,
    BigDecimal netAmount,
    String templateCode,
    int templateVersion,
    String ownerUsername,
    String ownerPermission,
    DispatchStatus dispatchStatus,
    String dispatchError,
    String creditOf,
    String remarks,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a service invoice.
   *
   * @param s service invoice
   * @return response
   */
  public static ServiceInvoiceResponse from(ServiceInvoice s) {
    return new ServiceInvoiceResponse(
        s.getId(),
        s.getSiNo(),
        s.getTypeCode(),
        s.getKind(),
        s.getInvoiceNo(),
        s.getArn(),
        s.getRecipientCode(),
        s.getRecipientName(),
        s.getRecipientEmail(),
        s.getIssueDate(),
        s.getCurrency(),
        s.getCommission(),
        s.getVatOnCommission(),
        s.getWtaxAmount(),
        s.getNetAmount(),
        s.getTemplateCode(),
        s.getTemplateVersion(),
        s.getOwnerUsername(),
        s.getOwnerPermission(),
        s.getDispatchStatus(),
        s.getDispatchError(),
        s.getCreditOf(),
        s.getRemarks(),
        s.getCreatedBy(),
        s.getCreatedAt());
  }
}
