package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate.Facts;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine.Kind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Request and response bodies of the received-certificate register (DIS 2.11). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ReceivedCertificateDtos {

  private ReceivedCertificateDtos() {}

  /**
   * An income payment of a certificate.
   *
   * @param kind commission or incentive
   * @param atc ATC
   * @param incomeNature nature of the income
   * @param income income payment
   * @param tax tax withheld
   */
  public record LineBody(
      @NotNull Kind kind,
      @NotBlank @Size(max = 10) String atc,
      @Size(max = 200) String incomeNature,
      @NotNull @DecimalMin("0") BigDecimal income,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal tax) {}

  /**
   * A certificate to record.
   *
   * @param certificateNo number
   * @param agentCode withholding agent (insurer party code)
   * @param agentName withholding agent name
   * @param agentTin withholding agent TIN
   * @param periodFrom first day covered
   * @param periodTo last day covered
   * @param receivedOn date received
   * @param sourceModule source module (e.g. DISBURSEMENT), blank for the tax screen
   * @param sourceRef source reference (e.g. the DV number)
   * @param remarks remarks
   * @param lines income payments
   */
  public record CertificateBody(
      @NotBlank @Size(max = 40) String certificateNo,
      @NotBlank @Size(max = 30) String agentCode,
      @NotBlank @Size(max = 200) String agentName,
      @Size(max = 20) String agentTin,
      @NotNull LocalDate periodFrom,
      @NotNull LocalDate periodTo,
      @NotNull LocalDate receivedOn,
      @Size(max = 30) String sourceModule,
      @Size(max = 80) String sourceRef,
      @Size(max = 250) String remarks,
      @NotEmpty @Valid List<LineBody> lines) {

    /**
     * The facts.
     *
     * @return facts
     */
    public Facts facts() {
      return new Facts(
          certificateNo.trim(),
          agentCode.trim(),
          agentName.trim(),
          blankToNull(agentTin),
          periodFrom,
          periodTo,
          receivedOn,
          blankToNull(sourceModule) == null ? "TAX" : sourceModule.trim(),
          blankToNull(sourceRef),
          blankToNull(remarks));
    }

    /**
     * The income payments, numbered.
     *
     * @return lines
     */
    public List<ReceivedCertificateLine> certificateLines() {
      List<ReceivedCertificateLine> out = new ArrayList<>();
      for (int i = 0; i < lines.size(); i++) {
        LineBody l = lines.get(i);
        out.add(
            new ReceivedCertificateLine(
                i + 1,
                l.kind(),
                l.atc().trim(),
                blankToNull(l.incomeNature()),
                l.income(),
                l.tax()));
      }
      return out;
    }

    private static String blankToNull(String s) {
      return s == null || s.isBlank() ? null : s.trim();
    }
  }

  /**
   * A received certificate.
   *
   * @param id id
   * @param certificateNo number
   * @param agentCode agent
   * @param agentName agent name
   * @param agentTin agent TIN
   * @param periodFrom first day covered
   * @param periodTo last day covered
   * @param receivedOn received on
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param status RECORDED or CANCELLED
   * @param incomeTotal income
   * @param taxTotal tax withheld
   * @param journalBatchNo posting
   * @param cancelJournalNo reversal
   * @param cancelReason cancellation reason
   * @param remarks remarks
   * @param lines income payments
   * @param recordedBy recorded by
   * @param recordedAt recorded at
   */
  public record CertificateResponse(
      Long id,
      String certificateNo,
      String agentCode,
      String agentName,
      String agentTin,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate receivedOn,
      String sourceModule,
      String sourceRef,
      String status,
      BigDecimal incomeTotal,
      BigDecimal taxTotal,
      String journalBatchNo,
      String cancelJournalNo,
      String cancelReason,
      String remarks,
      List<ReceivedCertificateLine> lines,
      String recordedBy,
      Instant recordedAt) {

    /**
     * Maps a certificate.
     *
     * @param c certificate
     * @return response
     */
    public static CertificateResponse from(ReceivedCertificate c) {
      return new CertificateResponse(
          c.getId(),
          c.getCertificateNo(),
          c.getAgentCode(),
          c.getAgentName(),
          c.getAgentTin(),
          c.getPeriodFrom(),
          c.getPeriodTo(),
          c.getReceivedOn(),
          c.getSourceModule(),
          c.getSourceRef(),
          c.getStatus(),
          c.getIncomeTotal(),
          c.getTaxTotal(),
          c.getJournalBatchNo(),
          c.getCancelJournalNo(),
          c.getCancelReason(),
          c.getRemarks(),
          c.getLines(),
          c.getCreatedBy(),
          c.getCreatedAt());
    }
  }
}
