package com.iortatechnxt.brokerverse.collections.billing.api.dto;

import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement.StatementStatus;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine.LineKind;
import com.iortatechnxt.brokerverse.collections.billing.service.SoaDispatch.Mail;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the billing statement endpoints (BRCLXN.058/060). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class BillingDtos {

  private BillingDtos() {}

  /**
   * A statement of account.
   *
   * @param id id
   * @param soaNo number
   * @param planId plan
   * @param cycleSeq billing cycle
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   * @param frequency billing frequency
   * @param cycleFrom cycle start
   * @param cycleTo cycle end
   * @param dueDate due date
   * @param total total billed
   * @param paid paid
   * @param balance amount due
   * @param status status
   * @param templateVersion template version used
   * @param sentAt sent
   * @param sentTo recipients
   * @param cancelReason cancellation reason
   * @param generatedBy user
   * @param createdAt generated
   * @param lines lines (detail only; empty in lists)
   */
  public record StatementResponse(
      Long id,
      String soaNo,
      Long planId,
      int cycleSeq,
      String arn,
      String clientCode,
      String assuredName,
      String currency,
      String frequency,
      LocalDate cycleFrom,
      LocalDate cycleTo,
      LocalDate dueDate,
      BigDecimal total,
      BigDecimal paid,
      BigDecimal balance,
      StatementStatus status,
      String templateVersion,
      Instant sentAt,
      String sentTo,
      String cancelReason,
      String generatedBy,
      Instant createdAt,
      List<LineResponse> lines) {

    /**
     * Maps a statement without its lines.
     *
     * @param s statement
     * @return DTO
     */
    public static StatementResponse from(BillingStatement s) {
      return of(s, List.of());
    }

    /**
     * Maps a statement with its lines (loaded).
     *
     * @param s statement
     * @return DTO
     */
    public static StatementResponse detail(BillingStatement s) {
      return of(s, s.getLines().stream().map(LineResponse::from).toList());
    }

    private static StatementResponse of(BillingStatement s, List<LineResponse> lines) {
      return new StatementResponse(
          s.getId(),
          s.getSoaNo(),
          s.getPlanId(),
          s.getCycleSeq(),
          s.getArn(),
          s.getClientCode(),
          s.getAssuredName(),
          s.getCurrency(),
          s.getFrequency(),
          s.getCycleFrom(),
          s.getCycleTo(),
          s.getDueDate(),
          s.getTotal(),
          s.getPaid(),
          s.getBalance(),
          s.getStatus(),
          s.getTemplateCode() == null ? null : s.getTemplateCode() + " v" + s.getTemplateVersion(),
          s.getSentAt(),
          s.getSentTo(),
          s.getCancelReason(),
          s.getCreatedBy(),
          s.getCreatedAt(),
          lines);
    }
  }

  /**
   * A line of a statement.
   *
   * @param lineNo line
   * @param kind current or arrears
   * @param invoiceNo invoice (null for a scheduled policy year)
   * @param policyYear policy year
   * @param installmentSeq installment
   * @param coverageFrom coverage start
   * @param coverageTo coverage end
   * @param dueDate due date
   * @param amount amount
   * @param paid paid
   * @param balance balance
   */
  public record LineResponse(
      int lineNo,
      LineKind kind,
      String invoiceNo,
      int policyYear,
      int installmentSeq,
      LocalDate coverageFrom,
      LocalDate coverageTo,
      LocalDate dueDate,
      BigDecimal amount,
      BigDecimal paid,
      BigDecimal balance) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return DTO
     */
    public static LineResponse from(BillingStatementLine l) {
      return new LineResponse(
          l.getLineNo(),
          l.getKind(),
          l.getInvoiceNo(),
          l.getPolicyYear(),
          l.getInstallmentSeq(),
          l.getCoverageFrom(),
          l.getCoverageTo(),
          l.getDueDate(),
          l.getAmount(),
          l.getPaid(),
          l.getBalance());
    }
  }

  /**
   * The statement of one billing cycle.
   *
   * @param planId plan
   * @param cycleSeq cycle (installment sequence)
   */
  public record GenerateRequest(@NotNull Long planId, @Min(1) int cycleSeq) {}

  /**
   * The billing run of a period.
   *
   * @param companyId company
   * @param from first due date
   * @param to last due date
   */
  public record GenerateDueRequest(
      @NotNull Long companyId, @NotNull LocalDate from, @NotNull LocalDate to) {}

  /**
   * An e-mail of a statement.
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body body
   * @param passwordHint how the recipient builds the password, may be null
   */
  public record SendRequest(
      @NotEmpty List<@NotBlank @Size(max = 200) String> to,
      List<@NotBlank @Size(max = 200) String> cc,
      @NotBlank @Size(max = 250) String subject,
      @NotBlank @Size(max = 4000) String body,
      @Size(max = 200) String passwordHint) {

    /**
     * The service mail.
     *
     * @return mail
     */
    public Mail toMail() {
      return new Mail(to, cc, subject, body, passwordHint);
    }
  }

  /**
   * The suggested recipient.
   *
   * @param email client e-mail, may be null
   */
  public record RecipientResponse(String email) {}
}
