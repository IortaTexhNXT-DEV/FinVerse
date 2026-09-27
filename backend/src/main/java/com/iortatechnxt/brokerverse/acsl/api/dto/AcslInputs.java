package com.iortatechnxt.brokerverse.acsl.api.dto;

import com.iortatechnxt.brokerverse.acsl.domain.CaseOutcome;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLineValues;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlControl;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.acsl.domain.SlSource;
import com.iortatechnxt.brokerverse.acsl.service.CaseService.CaseDraft;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Request bodies of the ACSL endpoints (ACSL 2.4-2.15). */
public interface AcslInputs {

  /**
   * A new case.
   *
   * @param type type
   * @param invoiceNo invoice
   * @param arNo AR of the payment
   * @param amount amount concerned
   * @param subject subject
   * @param details details
   */
  record CaseInput(
      @NotNull CaseType type,
      @Size(max = 40) String invoiceNo,
      @Size(max = 40) String arNo,
      @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @NotBlank @Size(max = 250) String subject,
      @Size(max = 2000) String details) {

    /**
     * The case draft.
     *
     * @return draft
     */
    public CaseDraft draft() {
      return new CaseDraft(type, invoiceNo, arNo, amount, subject, details);
    }
  }

  /**
   * Assignment.
   *
   * @param username assignee
   * @param comment comment
   */
  record AssignInput(@NotBlank @Size(max = 50) String username, @Size(max = 500) String comment) {}

  /**
   * Findings of an investigation.
   *
   * @param findings findings
   */
  record FindingsInput(@Size(max = 4000) String findings) {}

  /**
   * Result given to the requester.
   *
   * @param outcome outcome
   * @param remarks remarks
   */
  record ResultInput(@NotNull CaseOutcome outcome, @Size(max = 1000) String remarks) {}

  /**
   * A payment reversal request.
   *
   * @param receiptNo AR / OR of the payment
   * @param amount amount, empty for the whole application
   * @param reason reason
   */
  record ReversalInput(
      @NotBlank @Size(max = 40) String receiptNo,
      @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @NotBlank @Size(max = 250) String reason) {}

  /**
   * A message to the Account Officer.
   *
   * @param message message
   */
  record MessageInput(@NotBlank @Size(max = 1000) String message) {}

  /**
   * A comment given with an action.
   *
   * @param comment comment
   */
  record CommentInput(@Size(max = 500) String comment) {}

  /**
   * A new correction.
   *
   * @param kind kind (ACSL_CORRECTION_KIND)
   * @param invoiceNo invoice corrected
   * @param originalBatchNo journal corrected
   * @param description description
   */
  record CorrectionInput(
      @NotBlank @Size(max = 20) String kind,
      @Size(max = 40) String invoiceNo,
      @Size(max = 40) String originalBatchNo,
      @NotBlank @Size(max = 500) String description) {

    /**
     * The correction draft.
     *
     * @return draft
     */
    public CorrectionService.Draft draft() {
      return new CorrectionService.Draft(kind, invoiceNo, originalBatchNo, description);
    }
  }

  /**
   * A wrong-account proposal.
   *
   * @param batchNo original journal
   * @param lineNo original line
   * @param targetAccountCode right account
   * @param targetPartyCode right party
   * @param component ledger component of the reversed line
   * @param targetComponent ledger component of the re-posted line
   */
  record ProposalInput(
      @NotBlank @Size(max = 40) String batchNo,
      @Min(1) int lineNo,
      @NotBlank @Size(max = 30) String targetAccountCode,
      @Size(max = 30) String targetPartyCode,
      @Size(max = 20) String component,
      @Size(max = 20) String targetComponent) {

    /**
     * The proposal.
     *
     * @return proposal
     */
    public CorrectionService.Proposal proposal() {
      return new CorrectionService.Proposal(
          batchNo, lineNo, targetAccountCode, targetPartyCode, component, targetComponent);
    }
  }

  /**
   * One correction line.
   *
   * @param accountCode account
   * @param side side
   * @param amount amount
   * @param partyCode party
   * @param invoiceNo invoice
   * @param component ledger component
   * @param costCenter cost centre
   * @param businessLine line of business
   * @param narration narration
   * @param origin origin
   * @param originalBatchNo original journal
   * @param originalLineNo original line
   */
  record LineInput(
      @NotBlank @Size(max = 30) String accountCode,
      @NotNull BalanceSide side,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @Size(max = 30) String partyCode,
      @Size(max = 40) String invoiceNo,
      @Size(max = 20) String component,
      @Size(max = 20) String costCenter,
      @Size(max = 20) String businessLine,
      @Size(max = 250) String narration,
      LineOrigin origin,
      @Size(max = 40) String originalBatchNo,
      Integer originalLineNo) {

    /**
     * The line values.
     *
     * @return values
     */
    public CorrectionLineValues values() {
      return new CorrectionLineValues(
          accountCode,
          side,
          amount,
          partyCode,
          invoiceNo,
          component,
          costCenter,
          businessLine,
          narration,
          origin,
          originalBatchNo,
          originalLineNo);
    }
  }

  /**
   * The lines of a correction.
   *
   * @param lines lines
   */
  record LinesInput(@NotNull @Size(max = 200) List<@Valid LineInput> lines) {}

  /**
   * The sub-ledger of a control account.
   *
   * @param accountCode control account
   * @param source sub-ledger
   * @param components Operations components, comma separated
   * @param documentTypes open-item document types, comma separated
   * @param currency Operations invoice currency
   * @param active used or not
   */
  record ControlInput(
      @NotBlank @Size(max = 30) String accountCode,
      @NotNull SlSource source,
      @Size(max = 200) String components,
      @Size(max = 200) String documentTypes,
      @Size(max = 3) String currency,
      boolean active) {

    /**
     * The setting.
     *
     * @return setting
     */
    public GlSlControl.Setting setting() {
      return new GlSlControl.Setting(source, components, documentTypes, currency, active);
    }
  }
}
