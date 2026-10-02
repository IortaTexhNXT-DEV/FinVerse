package com.iortatechnxt.brokerverse.collections.installment.api.dto;

import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService.ManualEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the installment plan endpoints (BRCLXN.053/054/058). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class PlanDtos {

  private static final int MAX_INSTALLMENTS = 120;

  private PlanDtos() {}

  /**
   * An installment plan.
   *
   * @param id id
   * @param planNo number
   * @param arn account
   * @param invoiceNo invoice (null for a policy-year plan)
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   * @param frequency billing frequency
   * @param source source
   * @param firstDue first due date
   * @param installmentCount installments
   * @param total total
   * @param paidTotal allocated payments
   * @param balance total less paid
   * @param status status
   * @param remarks remarks
   * @param cancelReason cancellation reason
   * @param refreshedAt last allocation
   * @param createdBy creator
   * @param createdAt created
   * @param installments installments (detail only; empty in lists)
   */
  public record PlanResponse(
      Long id,
      String planNo,
      String arn,
      String invoiceNo,
      String clientCode,
      String assuredName,
      String currency,
      String frequency,
      PlanSource source,
      LocalDate firstDue,
      int installmentCount,
      BigDecimal total,
      BigDecimal paidTotal,
      BigDecimal balance,
      PlanStatus status,
      String remarks,
      String cancelReason,
      Instant refreshedAt,
      String createdBy,
      Instant createdAt,
      List<InstallmentResponse> installments) {

    /**
     * Maps a plan without its installments.
     *
     * @param p plan
     * @return DTO
     */
    public static PlanResponse from(InstallmentPlan p) {
      return of(p, List.of());
    }

    /**
     * Maps a plan with its installments (loaded).
     *
     * @param p plan
     * @return DTO
     */
    public static PlanResponse detail(InstallmentPlan p) {
      return of(p, p.getInstallments().stream().map(InstallmentResponse::from).toList());
    }

    private static PlanResponse of(InstallmentPlan p, List<InstallmentResponse> installments) {
      return new PlanResponse(
          p.getId(),
          p.getPlanNo(),
          p.getArn(),
          p.getInvoiceNo(),
          p.getClientCode(),
          p.getAssuredName(),
          p.getCurrency(),
          p.getFrequency(),
          p.getSource(),
          p.getFirstDue(),
          p.getInstallmentCount(),
          p.getTotal(),
          p.getPaidTotal(),
          p.getTotal().subtract(p.getPaidTotal()),
          p.getStatus(),
          p.getRemarks(),
          p.getCancelReason(),
          p.getRefreshedAt(),
          p.getCreatedBy(),
          p.getCreatedAt(),
          installments);
    }
  }

  /**
   * An installment (one billing cycle).
   *
   * @param id id
   * @param seq sequence
   * @param policyYear policy year
   * @param invoiceNo invoice billed (null while the year is scheduled)
   * @param dueDate due date
   * @param cycleFrom cycle start
   * @param cycleTo cycle end
   * @param amount amount
   * @param paidAmount allocated payments
   * @param balance amount less paid
   * @param status status
   * @param overdueSince overdue since
   * @param paidOn paid on
   */
  public record InstallmentResponse(
      Long id,
      int seq,
      int policyYear,
      String invoiceNo,
      LocalDate dueDate,
      LocalDate cycleFrom,
      LocalDate cycleTo,
      BigDecimal amount,
      BigDecimal paidAmount,
      BigDecimal balance,
      InstallmentStatus status,
      LocalDate overdueSince,
      LocalDate paidOn) {

    /**
     * Maps an installment.
     *
     * @param i installment
     * @return DTO
     */
    public static InstallmentResponse from(Installment i) {
      return new InstallmentResponse(
          i.getId(),
          i.getSeq(),
          i.getPolicyYear(),
          i.getInvoiceNo(),
          i.getDueDate(),
          i.getCycleFrom(),
          i.getCycleTo(),
          i.getAmount(),
          i.getPaidAmount(),
          i.balance(),
          i.getStatus(),
          i.getOverdueSince(),
          i.getPaidOn());
    }
  }

  /**
   * An installment due or overdue, with its plan (work list).
   *
   * @param planId plan
   * @param planNo plan number
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   * @param installment the installment
   */
  public record DueInstallmentResponse(
      Long planId,
      String planNo,
      String arn,
      String clientCode,
      String assuredName,
      String currency,
      InstallmentResponse installment) {

    /**
     * Maps an installment with its plan (fetched).
     *
     * @param i installment
     * @return DTO
     */
    public static DueInstallmentResponse from(Installment i) {
      InstallmentPlan p = i.getPlan();
      return new DueInstallmentResponse(
          p.getId(),
          p.getPlanNo(),
          p.getArn(),
          p.getClientCode(),
          p.getAssuredName(),
          p.getCurrency(),
          InstallmentResponse.from(i));
    }
  }

  /**
   * A plan over the policy years of a multi-year account.
   *
   * @param companyId company
   * @param arn account
   * @param frequency billing frequency
   * @param remarks remarks
   */
  public record PolicyYearsRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) String arn,
      @NotBlank @Size(max = 40) String frequency,
      @Size(max = 500) String remarks) {}

  /**
   * A plan splitting an invoice's outstanding premium.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param frequency billing frequency
   * @param firstDue first due date
   * @param count installments
   * @param remarks remarks
   */
  public record GeneratedRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      @NotBlank @Size(max = 40) String frequency,
      @NotNull LocalDate firstDue,
      @Min(1) @Max(MAX_INSTALLMENTS) int count,
      @Size(max = 500) String remarks) {}

  /**
   * A plan with installments entered by the collector.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param frequency billing frequency shown on statements
   * @param entries installments
   * @param remarks remarks
   */
  public record ManualRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      @NotBlank @Size(max = 40) String frequency,
      @NotEmpty @Size(max = MAX_INSTALLMENTS) List<@Valid EntryRequest> entries,
      @Size(max = 500) String remarks) {

    /**
     * The entries as service values.
     *
     * @return entries
     */
    public List<ManualEntry> toEntries() {
      return entries.stream().map(e -> new ManualEntry(e.dueDate(), e.amount())).toList();
    }
  }

  /**
   * One installment entered by the collector.
   *
   * @param dueDate due date
   * @param amount amount
   */
  public record EntryRequest(@NotNull LocalDate dueDate, @NotNull @Positive BigDecimal amount) {}
}
