package com.iortatechnxt.brokerverse.screening.str.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.api.dto.ReviewDto;
import com.iortatechnxt.brokerverse.screening.str.domain.StrExtraction;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import com.iortatechnxt.brokerverse.screening.str.service.StrService.StrEdit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The DTOs of the STR screens (SNSRP-705, 706; FR-SS-070 to 072). */
public final class StrDtos {

  private StrDtos() {}

  /**
   * An STR of the register.
   *
   * @param id STR id
   * @param strNo STR number
   * @param caseId case
   * @param subjectCode client code
   * @param subjectName client name
   * @param status status
   * @param reasonCodes reason codes
   * @param committeeDecidedAt committee APPROVE_STR decision time
   * @param readyAt marked ready
   * @param extractionId extraction
   * @param extractedAt extracted
   * @param amlcReference AMLC reference
   * @param filedOn filing date
   * @param createdAt prepared
   * @param createdBy prepared by
   */
  public record StrRow(
      Long id,
      String strNo,
      Long caseId,
      String subjectCode,
      String subjectName,
      String status,
      Set<String> reasonCodes,
      Instant committeeDecidedAt,
      Instant readyAt,
      Long extractionId,
      Instant extractedAt,
      String amlcReference,
      LocalDate filedOn,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps an STR.
     *
     * @param s the STR
     * @return the row
     */
    public static StrRow from(SuspiciousTransactionReport s) {
      return new StrRow(
          s.getId(),
          s.getStrNo(),
          s.getCaseId(),
          s.getSubjectCode(),
          s.getSubjectName(),
          s.getStatus().name(),
          s.reasons(),
          s.getCommitteeDecidedAt(),
          s.getReadyAt(),
          s.getExtractionId(),
          s.getExtractedAt(),
          s.getAmlcReference(),
          s.getFiledOn(),
          s.getCreatedAt(),
          s.getCreatedBy());
    }
  }

  /**
   * An STR with its snapshot, template, values, transactions and gaps (STR tab).
   *
   * @param str the register facts
   * @param subjectSnapshot the subject at preparation
   * @param templateVersionId template version
   * @param fields template fields
   * @param values field values by code
   * @param transactions transactions
   * @param gaps completeness gaps by field
   */
  public record StrDetail(
      StrRow str,
      String subjectSnapshot,
      Long templateVersionId,
      List<ReviewDto.FieldDto> fields,
      Map<String, String> values,
      List<Transaction> transactions,
      Map<String, String> gaps) {}

  /**
   * An STR transaction.
   *
   * @param reference account, invoice, receipt or policy reference
   * @param date date
   * @param amount amount (greater than 0)
   * @param currency currency
   * @param type type
   * @param description description
   */
  public record Transaction(
      @Size(max = 60) String reference,
      LocalDate date,
      BigDecimal amount,
      @Size(max = 3) String currency,
      @Size(max = 30) String type,
      @Size(max = 500) String description) {

    /**
     * Maps a line.
     *
     * @param l the line
     * @return the DTO
     */
    public static Transaction from(Line l) {
      return new Transaction(
          l.reference(), l.date(), l.amount(), l.currency(), l.type(), l.description());
    }

    /**
     * The service line.
     *
     * @return line
     */
    public Line line() {
      return new Line(
          reference == null ? null : reference.strip(),
          date,
          amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP),
          currency == null ? null : currency.strip(),
          type == null || type.isBlank() ? "OTHER" : type.strip(),
          description);
    }
  }

  /**
   * Saves a draft STR.
   *
   * @param values template field values by code
   * @param reasonCodes reason codes
   * @param transactions the transactions (replace the list)
   */
  public record Save(
      Map<String, String> values, Set<String> reasonCodes, List<@Valid Transaction> transactions) {

    /**
     * The service edit.
     *
     * @return edit
     */
    public StrEdit edit() {
      return new StrEdit(
          values,
          reasonCodes,
          transactions == null ? List.of() : transactions.stream().map(Transaction::line).toList());
    }
  }

  /**
   * Extract the approved STRs of a period (FR-SS-071).
   *
   * @param companyId company
   * @param from period start
   * @param to period end
   * @param reason re-extraction reason, blank for a first extraction
   */
  public record Extract(
      Long companyId, LocalDate from, LocalDate to, @Size(max = 1000) String reason) {}

  /**
   * Record the AMLC filing (FR-SS-072).
   *
   * @param reference AMLC reference
   * @param filedOn filing date
   */
  public record Filing(@Size(max = 60) String reference, LocalDate filedOn) {}

  /**
   * An extraction.
   *
   * @param id extraction id
   * @param batchNo batch number
   * @param periodFrom period start
   * @param periodTo period end
   * @param strCount STRs
   * @param fileName file
   * @param sha256 file hash
   * @param reExtraction whether a re-extraction
   * @param reason re-extraction reason
   * @param extractedBy user
   * @param extractedAt time
   */
  public record ExtractionDto(
      Long id,
      String batchNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      int strCount,
      String fileName,
      String sha256,
      boolean reExtraction,
      String reason,
      String extractedBy,
      Instant extractedAt) {

    /**
     * Maps an extraction.
     *
     * @param x the extraction
     * @return the DTO
     */
    public static ExtractionDto from(StrExtraction x) {
      return new ExtractionDto(
          x.getId(),
          x.getBatchNo(),
          x.getPeriodFrom(),
          x.getPeriodTo(),
          x.getStrCount(),
          x.getFileName(),
          x.getSha256(),
          x.isReExtraction(),
          x.getReason(),
          x.getExtractedBy(),
          x.getExtractedAt());
    }
  }
}
