package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import java.time.Instant;
import java.util.List;

/**
 * One quotation with the content of its current version.
 *
 * @param id id
 * @param companyId company
 * @param quotationNo quotation number (Proposal No.)
 * @param arn Account Reference Number
 * @param requestId request answered
 * @param clientId client
 * @param clientCode client or prospect code
 * @param clientName client name
 * @param clientEmail client e-mail
 * @param productCode product
 * @param lineCode product line
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param currency currency
 * @param templateVersion intake template version (BRNB.004)
 * @param currentVersion current version number
 * @param versionOpen whether the current version may still change
 * @param status status
 * @param tsuRequired whether a TSU routing rule applies
 * @param tsuReason rule description
 * @param submittedBy submitter
 * @param submittedAt submission time
 * @param approvedBy approver
 * @param approvedAt approval time
 * @param sentAt sent to the client
 * @param acceptedAt accepted by the client
 * @param acceptedGroups accepted risk groups
 * @param accountArns accounts created
 * @param createdBy maker
 * @param createdAt created
 * @param content current content
 * @param productVersionNo package version that priced the current version (BRPM.007)
 * @param rateOverrideRef approved rate-scheme exception used, null when none
 */
public record QuotationResponse(
    Long id,
    Long companyId,
    String quotationNo,
    String arn,
    Long requestId,
    Long clientId,
    String clientCode,
    String clientName,
    String clientEmail,
    String productCode,
    String lineCode,
    String marketSegment,
    String sourceChannel,
    String currency,
    String templateVersion,
    int currentVersion,
    boolean versionOpen,
    QuotationStatus status,
    boolean tsuRequired,
    String tsuReason,
    String submittedBy,
    Instant submittedAt,
    String approvedBy,
    Instant approvedAt,
    Instant sentAt,
    Instant acceptedAt,
    List<Integer> acceptedGroups,
    List<String> accountArns,
    String createdBy,
    Instant createdAt,
    QuotationContentResponse content,
    Integer productVersionNo,
    String rateOverrideRef) {

  /**
   * Maps a quotation (account references loaded) with its current content.
   *
   * @param q quotation
   * @param content current content
   * @return response
   */
  public static QuotationResponse from(Quotation q, QuotationContent content) {
    return new QuotationResponse(
        q.getId(),
        q.getCompanyId(),
        q.getQuotationNo(),
        q.getArn(),
        q.getRequestId(),
        q.getClientId(),
        q.getClientCode(),
        q.getClientName(),
        q.getClientEmail(),
        q.getProductCode(),
        q.getLineCode(),
        q.getMarketSegment(),
        q.getSourceChannel(),
        q.getCurrency(),
        q.getTemplateVersion(),
        q.getCurrentVersion(),
        q.isVersionOpen(),
        q.getStatus(),
        q.isTsuRequired(),
        q.getTsuReason(),
        q.getSubmittedBy(),
        q.getSubmittedAt(),
        q.getApprovedBy(),
        q.getApprovedAt(),
        q.getSentAt(),
        q.getAcceptedAt(),
        q.getAcceptedGroupList(),
        List.copyOf(q.getAccountArns()),
        q.getCreatedBy(),
        q.getCreatedAt(),
        QuotationContentResponse.from(content),
        content.schemeVersion(),
        q.getRateOverrideRef());
  }
}
