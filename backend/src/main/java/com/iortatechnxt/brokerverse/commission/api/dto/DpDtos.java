package com.iortatechnxt.brokerverse.commission.api.dto;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.service.DpListService.Submissions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Request and response records of the direct payment commission API (CMRID.001-013). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class DpDtos {

  private DpDtos() {}

  /**
   * A DP list.
   *
   * @param id id
   * @param listNo list number
   * @param source branch, head office or feed
   * @param branchCode branch
   * @param submissionDate submission date
   * @param fileName file
   * @param runNo flow-in run
   * @param itemCount accounts
   * @param validCount valid accounts
   * @param excludedCount excluded accounts
   * @param createdAt received at
   * @param createdBy received by
   */
  public record DpListResponse(
      Long id,
      String listNo,
      ListSource source,
      String branchCode,
      LocalDate submissionDate,
      String fileName,
      String runNo,
      int itemCount,
      int validCount,
      int excludedCount,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a list.
     *
     * @param l list
     * @return response
     */
    public static DpListResponse from(DpList l) {
      return new DpListResponse(
          l.getId(),
          l.getListNo(),
          l.getSource(),
          l.getBranchCode(),
          l.getSubmissionDate(),
          l.getFileName(),
          l.getRunNo(),
          l.getItemCount(),
          l.getValidCount(),
          l.getExcludedCount(),
          l.getCreatedAt(),
          l.getCreatedBy());
    }
  }

  /**
   * A branch in the submission tracker.
   *
   * @param branchCode branch
   * @param branchName name
   * @param lists lists received
   * @param accounts accounts received
   * @param lastSubmission last submission
   * @param received whether the branch submitted
   */
  public record SubmissionResponse(
      String branchCode,
      String branchName,
      int lists,
      int accounts,
      LocalDate lastSubmission,
      boolean received) {

    /**
     * Maps a tracker row.
     *
     * @param s row
     * @return response
     */
    public static SubmissionResponse from(Submissions s) {
      return new SubmissionResponse(
          s.branchCode(),
          s.branchName(),
          s.lists(),
          s.accounts(),
          s.lastSubmission(),
          s.received());
    }
  }

  /**
   * One validation rule result.
   *
   * @param rule rule
   * @param passed passed
   * @param message detail
   */
  public record RuleResult(String rule, boolean passed, String message) {

    /**
     * Reads the stored results ("RULE|PASS|detail" per line).
     *
     * @param text stored results
     * @return results
     */
    public static List<RuleResult> parse(String text) {
      if (text == null || text.isBlank()) {
        return List.of();
      }
      return Arrays.stream(text.split("\n"))
          .map(line -> line.split("\\|", -1))
          .map(
              p ->
                  new RuleResult(
                      p[0], p.length > 1 && "PASS".equals(p[1]), p.length > 2 ? p[2] : ""))
          .toList();
    }
  }

  /**
   * A direct payment account.
   *
   * @param id id
   * @param listId list
   * @param invoiceNo invoice
   * @param policyNo policy
   * @param insurerCode insurer
   * @param clientCode client
   * @param assuredName assured
   * @param branchCode submitting branch
   * @param submittedPremium premium submitted
   * @param amounts commission receivable from the ledger
   * @param tag tag
   * @param sanitation sanitation
   * @param rules rule results
   * @param billingId billing
   * @param feedback insurer feedback and collection
   */
  public record DpItemResponse(
      Long id,
      Long listId,
      String invoiceNo,
      String policyNo,
      String insurerCode,
      String clientCode,
      String assuredName,
      String branchCode,
      BigDecimal submittedPremium,
      Amounts amounts,
      DpTag tag,
      Sanitation sanitation,
      List<RuleResult> rules,
      Long billingId,
      Progress feedback) {

    /**
     * Maps an account.
     *
     * @param i account
     * @return response
     */
    public static DpItemResponse from(DpItem i) {
      return new DpItemResponse(
          i.getId(),
          i.getListId(),
          i.getInvoiceNo(),
          i.getPolicyNo(),
          i.getInsurerCode(),
          i.getClientCode(),
          i.getAssuredName(),
          i.getBranchCode(),
          i.getSubmittedPremium(),
          new Amounts(
              i.getPremium(),
              i.getCommission(),
              i.getCommissionVat(),
              i.getWtax(),
              i.getNetCommission()),
          i.getTag(),
          i.getSanitation(),
          RuleResult.parse(i.getRuleResults()),
          i.getBillingId(),
          new Progress(
              i.getRemarks(),
              i.getConfirmedBy(),
              i.getConfirmedAt(),
              i.getFeedbackReason(),
              i.getFeedbackComment(),
              i.getRespondedAt(),
              i.getReturnedRef(),
              i.getOrNo(),
              i.getCollectedAmount(),
              i.getCollectedOn(),
              i.getPrReversedAt(),
              i.getReinstatedCount()));
    }
  }

  /**
   * Commission receivable of an account (CMRID.007).
   *
   * @param premium gross premium
   * @param commission commission
   * @param commissionVat VAT on commission
   * @param wtax withholding tax
   * @param net net commission
   */
  public record Amounts(
      BigDecimal premium,
      BigDecimal commission,
      BigDecimal commissionVat,
      BigDecimal wtax,
      BigDecimal net) {}

  /**
   * What happened to an account since its intake.
   *
   * @param remarks remarks or exclusion reason
   * @param confirmedBy reviewer
   * @param confirmedAt confirmed at
   * @param feedbackReason insurer's reason
   * @param feedbackComment insurer's comment
   * @param respondedAt answered at
   * @param returnedRef return to Collection or unapplied reference
   * @param orNo commission OR
   * @param collectedAmount amount collected
   * @param collectedOn collection date
   * @param prReversedAt PR reversed at
   * @param reinstatedCount reinstatements
   */
  public record Progress(
      String remarks,
      String confirmedBy,
      Instant confirmedAt,
      String feedbackReason,
      String feedbackComment,
      Instant respondedAt,
      String returnedRef,
      String orNo,
      BigDecimal collectedAmount,
      LocalDate collectedOn,
      Instant prReversedAt,
      int reinstatedCount) {}

  /**
   * A commission billing.
   *
   * @param id id
   * @param billingNo billing number
   * @param insurerCode insurer
   * @param handler handler
   * @param stage stage
   * @param itemCount accounts
   * @param amounts totals
   * @param fileId billing file
   * @param fileName billing file name
   * @param sentAt sent at
   * @param slaDue feedback due date
   * @param overdue feedback overdue
   * @param respondedAt answered at
   * @param recipients recipients
   * @param orNo commission OR
   * @param orStatus ISSUED or DEFERRED
   * @param createdAt created at
   */
  public record BillingResponse(
      Long id,
      String billingNo,
      String insurerCode,
      String handler,
      String stage,
      int itemCount,
      Amounts amounts,
      Long fileId,
      String fileName,
      Instant sentAt,
      LocalDate slaDue,
      boolean overdue,
      Instant respondedAt,
      String recipients,
      String orNo,
      String orStatus,
      Instant createdAt) {

    /**
     * Maps a billing.
     *
     * @param b billing
     * @param today today (overdue flag)
     * @return response
     */
    public static BillingResponse from(DpBilling b, LocalDate today) {
      return new BillingResponse(
          b.getId(),
          b.getBillingNo(),
          b.getInsurerCode(),
          b.getHandler(),
          b.getStage(),
          b.getItemCount(),
          new Amounts(
              null, b.getTotalCommission(), b.getTotalVat(), b.getTotalWtax(), b.getTotalNet()),
          b.getFileId(),
          b.getFileName(),
          b.getSentAt(),
          b.getSlaDue(),
          b.isOverdue(today),
          b.getRespondedAt(),
          b.getRecipients(),
          b.getOrNo(),
          b.getOrStatus(),
          b.getCreatedAt());
    }
  }

  /**
   * Accounts selected.
   *
   * @param ids accounts
   */
  public record IdsRequest(@NotEmpty List<Long> ids) {}

  /**
   * Accounts to exclude.
   *
   * @param ids accounts
   * @param reason reason
   */
  public record ExcludeRequest(
      @NotEmpty List<Long> ids, @NotBlank @Size(max = 500) String reason) {}

  /**
   * Accounts to bill.
   *
   * @param companyId company
   * @param ids accounts; every account for billing when empty
   */
  public record PrepareRequest(@NotNull Long companyId, List<Long> ids) {}

  /**
   * Recipients of a billing.
   *
   * @param to recipients; the insurer's addresses when empty
   * @param cc copy recipients
   */
  public record SendRequest(List<@NotBlank String> to, List<@NotBlank String> cc) {}

  /**
   * The insurer's answers.
   *
   * @param answers one per account
   */
  public record AnswersRequest(@NotEmpty List<@Valid AnswerRequest> answers) {}

  /**
   * The insurer's answer on an account.
   *
   * @param invoiceNo invoice
   * @param approved approved or rejected
   * @param reason reason of a rejection (LOV DP_FEEDBACK_REASON)
   * @param comment comment
   */
  public record AnswerRequest(
      @NotBlank String invoiceNo,
      boolean approved,
      @Size(max = 40) String reason,
      @Size(max = 500) String comment) {}

  /**
   * A collection.
   *
   * @param receiptDate collection date, today when null
   * @param bankAccount GL bank account, the parameter's when blank
   * @param certificateRef BIR certificate, may be null
   */
  public record CollectRequest(
      LocalDate receiptDate,
      @Size(max = 30) String bankAccount,
      @Size(max = 60) String certificateRef) {}

  /**
   * A reinstatement.
   *
   * @param reasonCode reason (LOV REINSTATEMENT_REASON, DP_ codes)
   * @param comment comment
   */
  public record ReinstateRequest(
      @NotBlank @Size(max = 40) String reasonCode, @Size(max = 500) String comment) {}

  /**
   * A comment.
   *
   * @param comment comment, may be null
   */
  public record CommentRequest(@Size(max = 500) String comment) {}

  /**
   * Commission settings shown on the screens.
   *
   * @param feedbackWorkingDays insurer feedback working days (CMR_FEEDBACK_WORKING_DAYS)
   * @param collectionBank proposed bank account (CMR_DP_COLLECTION_BANK)
   * @param prReversalPosting whether the PR reversal is posted to the GL
   */
  public record SettingsResponse(
      int feedbackWorkingDays, String collectionBank, boolean prReversalPosting) {}
}
