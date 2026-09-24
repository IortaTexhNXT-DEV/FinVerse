package com.iortatechnxt.brokerverse.nonpackage.api.dto;

import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseHistory;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseTerms;
import com.iortatechnxt.brokerverse.nonpackage.service.ComparativeTable;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService.ChecklistItem;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService.ClientEmail;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the PRF, TSU queue and slip endpoints. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ProposalDtos {

  private ProposalDtos() {}

  /**
   * A row of the PRF list.
   *
   * @param id id
   * @param prfNo marketing reference (Proposal No.)
   * @param arn ARN
   * @param clientId client
   * @param clientCode client code
   * @param clientName client name
   * @param productCode product
   * @param totalSumInsured total sum insured
   * @param chosenInsurer chosen insurer
   * @param qsNo quotation slip number
   * @param psNo proposal slip number
   * @param status status
   * @param createdBy maker
   * @param createdAt created
   */
  public record ListItem(
      Long id,
      String prfNo,
      String arn,
      Long clientId,
      String clientCode,
      String clientName,
      String productCode,
      BigDecimal totalSumInsured,
      String chosenInsurer,
      String qsNo,
      String psNo,
      ProposalStatus status,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a PRF.
     *
     * @param p PRF
     * @return row
     */
    public static ListItem from(ProposalRequest p) {
      return new ListItem(
          p.getId(),
          p.getPrfNo(),
          p.getArn(),
          p.getClientId(),
          p.getClientCode(),
          p.getClientName(),
          p.getProductCode(),
          p.getTotalSumInsured(),
          p.getChosenInsurer(),
          p.getQsNo(),
          p.getPsNo(),
          p.getStatus(),
          p.getCreatedBy(),
          p.getCreatedAt());
    }
  }

  /**
   * Insurers selected for the quotation slip.
   *
   * @param insurers insurer party codes
   */
  public record InsurersBody(@NotEmpty @Size(max = 20) List<@NotBlank String> insurers) {}

  /**
   * Quotation slip submission.
   *
   * @param replyBy reply date asked from the insurers; default when empty
   * @param comment comment
   */
  public record SlipSubmitBody(LocalDate replyBy, @Size(max = 1000) String comment) {}

  /**
   * Proposal slip submission.
   *
   * @param insurerCode chosen insurer; the recommended one when empty
   * @param comment comment
   */
  public record ProposalSlipBody(
      @Size(max = 30) String insurerCode, @Size(max = 1000) String comment) {}

  /**
   * Terms of an insurer (BRNB.009).
   *
   * @param status received or declined
   * @param premium premium
   * @param rate rate in percent
   * @param deductibles deductibles
   * @param conditions conditions
   * @param validUntil validity
   * @param remarks remarks
   */
  public record TermsBody(
      @NotNull ResponseStatus status,
      @DecimalMin("0") BigDecimal premium,
      @DecimalMin("0") @DecimalMax("100") BigDecimal rate,
      @Size(max = 1000) String deductibles,
      @Size(max = 2000) String conditions,
      LocalDate validUntil,
      @Size(max = 1000) String remarks) {

    /**
     * As terms.
     *
     * @return terms
     */
    public ResponseTerms terms() {
      return new ResponseTerms(status, premium, rate, deductibles, conditions, validUntil, remarks);
    }
  }

  /**
   * Closing the request for terms.
   *
   * @param closePending proceed although some insurers have not answered
   * @param comment comment
   */
  public record TermsCompleteBody(boolean closePending, @Size(max = 1000) String comment) {}

  /**
   * Proposal sent to the client (always password protected).
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body message
   * @param passwordHint password hint
   */
  public record SendBody(
      @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 120) String> to,
      @Size(max = 20) List<@NotBlank @Size(max = 120) String> cc,
      @NotBlank @Size(max = 300) String subject,
      @NotBlank @Size(max = 10000) String body,
      @Size(max = 300) String passwordHint) {

    /**
     * As an e-mail.
     *
     * @return e-mail
     */
    public ClientEmail email() {
      return new ClientEmail(to, cc, subject, body, passwordHint);
    }
  }

  /**
   * The client's acceptance.
   *
   * @param groups accepted risk groups; empty for all
   * @param comment comment
   */
  public record AcceptBody(
      @Size(max = 99) List<@Min(1) @Max(99) Integer> groups, @Size(max = 1000) String comment) {}

  /**
   * A response of an insurer.
   *
   * @param id response id
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param status status
   * @param revision revision
   * @param premium premium
   * @param rate rate
   * @param deductibles deductibles
   * @param conditions conditions
   * @param validUntil validity
   * @param remarks remarks
   * @param documentId response document (attachment)
   * @param recommended recommended flag
   * @param respondedAt keyed in
   * @param updatedBy last changed by
   */
  public record ResponseView(
      Long id,
      String insurerCode,
      String insurerName,
      ResponseStatus status,
      int revision,
      BigDecimal premium,
      BigDecimal rate,
      String deductibles,
      String conditions,
      LocalDate validUntil,
      String remarks,
      Long documentId,
      boolean recommended,
      Instant respondedAt,
      String updatedBy) {

    /**
     * Maps a response.
     *
     * @param r response
     * @return view
     */
    public static ResponseView from(InsurerResponse r) {
      return new ResponseView(
          r.getId(),
          r.getInsurerCode(),
          r.getInsurerName(),
          r.getStatus(),
          r.getRevision(),
          r.getPremium(),
          r.getRate(),
          r.getDeductibles(),
          r.getConditions(),
          r.getValidUntil(),
          r.getRemarks(),
          r.getDocumentId(),
          r.isRecommended(),
          r.getRespondedAt(),
          r.getUpdatedBy());
    }
  }

  /**
   * A snapshot of a response (version history).
   *
   * @param responseId response
   * @param revision revision
   * @param status status
   * @param premium premium
   * @param rate rate
   * @param validUntil validity
   * @param recommended recommended
   * @param changedBy changed by
   * @param changedAt changed at
   */
  public record HistoryView(
      Long responseId,
      int revision,
      ResponseStatus status,
      BigDecimal premium,
      BigDecimal rate,
      LocalDate validUntil,
      boolean recommended,
      String changedBy,
      Instant changedAt) {

    /**
     * Maps a snapshot.
     *
     * @param h snapshot
     * @return view
     */
    public static HistoryView from(InsurerResponseHistory h) {
      return new HistoryView(
          h.getResponseId(),
          h.getRevision(),
          h.getStatus(),
          h.getPremium(),
          h.getRate(),
          h.getValidUntil(),
          h.isRecommended(),
          h.getChangedBy(),
          h.getChangedAt());
    }
  }

  /**
   * The comparative table (BRNB.010).
   *
   * @param rows insurers side by side
   * @param recommendedInsurer recommended insurer
   */
  public record ComparativeView(List<ComparativeTable.Row> rows, String recommendedInsurer) {

    /**
     * Maps a table.
     *
     * @param t table
     * @return view
     */
    public static ComparativeView from(ComparativeTable t) {
      return new ComparativeView(t.rows(), t.recommendedInsurer());
    }
  }

  /**
   * A mandatory document of the product and whether it is attached (BRNB.005).
   *
   * @param documentType document type
   * @param attached attached
   */
  public record ChecklistView(String documentType, boolean attached) {

    /**
     * Maps a checklist item.
     *
     * @param c item
     * @return view
     */
    public static ChecklistView from(ChecklistItem c) {
      return new ChecklistView(c.documentType(), c.attached());
    }
  }
}
