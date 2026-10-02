package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDiff;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.BatchResult;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.EmailRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService.Preview;
import com.iortatechnxt.brokerverse.quotation.service.QuotationSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the quotation actions. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class QuotationActionDtos {

  private QuotationActionDtos() {}

  /**
   * Send a quotation to the client (always password protected, BRNB.013).
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body message
   * @param passwordHint password hint
   */
  public record SendRequest(
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
    public EmailRequest email() {
      return new EmailRequest(to, cc, subject, body, passwordHint);
    }
  }

  /**
   * Send several approved quotations (one e-mail per client, BRNB.042).
   *
   * @param ids quotations
   * @param subject subject, default when blank
   * @param body message, default when blank
   * @param passwordHint password hint
   */
  public record BatchSendRequest(
      @NotEmpty @Size(max = 200) List<Long> ids,
      @Size(max = 300) String subject,
      @Size(max = 10000) String body,
      @Size(max = 300) String passwordHint) {

    /**
     * The message.
     *
     * @return e-mail without recipients (each client's e-mail is used)
     */
    public EmailRequest email() {
      return new EmailRequest(List.of(), List.of(), subject, body, passwordHint);
    }
  }

  /**
   * Outcome of a batch send.
   *
   * @param quotations quotations sent
   * @param clients clients e-mailed
   * @param references quotation numbers
   */
  public record BatchSendResponse(int quotations, int clients, List<String> references) {

    /**
     * Maps a batch result.
     *
     * @param r result
     * @return response
     */
    public static BatchSendResponse from(BatchResult r) {
      return new BatchSendResponse(r.quotations(), r.emails(), r.references());
    }
  }

  /**
   * The client's acceptance (BRNB.045).
   *
   * @param groups accepted risk groups; empty for all
   * @param comment comment
   */
  public record AcceptRequest(
      @Size(max = 99) List<@Min(1) @Max(99) Integer> groups, @Size(max = 1000) String comment) {}

  /**
   * A priced draft (live premium).
   *
   * @param content priced content
   * @param tsuRequired whether a TSU routing rule applies
   * @param tsuReason rule description
   */
  public record PreviewResponse(
      QuotationContentResponse content, boolean tsuRequired, String tsuReason) {

    /**
     * Maps a preview.
     *
     * @param p preview
     * @return response
     */
    public static PreviewResponse from(Preview p) {
      return new PreviewResponse(
          QuotationContentResponse.from(p.content()), p.tsuRequired(), p.tsuReason());
    }
  }

  /**
   * Differences between two versions.
   *
   * @param fromVersion older version
   * @param toVersion newer version
   * @param fields changed terms
   * @param items item changes
   * @param grossFrom older gross premium
   * @param grossTo newer gross premium
   * @param grossDelta difference
   */
  public record DiffResponse(
      int fromVersion,
      int toVersion,
      List<QuotationDiff.FieldChange> fields,
      List<QuotationDiff.ItemChange> items,
      BigDecimal grossFrom,
      BigDecimal grossTo,
      BigDecimal grossDelta) {

    /**
     * Maps a diff.
     *
     * @param d diff
     * @return response
     */
    public static DiffResponse from(QuotationDiff d) {
      return new DiffResponse(
          d.fromVersion(),
          d.toVersion(),
          d.fields(),
          d.items(),
          d.grossFrom(),
          d.grossTo(),
          d.grossDelta());
    }
  }

  /**
   * A quotation by ARN (contract for Operations Cashiering, with the direct-payment flag).
   *
   * @param id id
   * @param quotationNo quotation number
   * @param arn ARN
   * @param clientCode client code
   * @param clientName client name
   * @param productCode product
   * @param insurerCode insurer
   * @param status status
   * @param currency currency
   * @param grossPremium gross premium
   * @param validUntil validity
   * @param directPayment direct payment to the insurer (MKTID.011)
   * @param accountArns accounts created
   */
  public record SummaryResponse(
      Long id,
      String quotationNo,
      String arn,
      String clientCode,
      String clientName,
      String productCode,
      String insurerCode,
      QuotationStatus status,
      String currency,
      BigDecimal grossPremium,
      LocalDate validUntil,
      boolean directPayment,
      List<String> accountArns) {

    /**
     * Maps a summary.
     *
     * @param s summary
     * @return response
     */
    public static SummaryResponse from(QuotationSummary s) {
      return new SummaryResponse(
          s.id(),
          s.quotationNo(),
          s.arn(),
          s.clientCode(),
          s.clientName(),
          s.productCode(),
          s.insurerCode(),
          s.status(),
          s.currency(),
          s.grossPremium(),
          s.validUntil(),
          s.directPayment(),
          s.accountArns());
    }
  }

  /**
   * Content of one version.
   *
   * @param versionNo version number
   * @param content content
   */
  public record VersionContentResponse(int versionNo, QuotationContentResponse content) {

    /**
     * Maps a version's content.
     *
     * @param versionNo version number
     * @param content content
     * @return response
     */
    public static VersionContentResponse from(int versionNo, QuotationContent content) {
      return new VersionContentResponse(versionNo, QuotationContentResponse.from(content));
    }
  }
}
