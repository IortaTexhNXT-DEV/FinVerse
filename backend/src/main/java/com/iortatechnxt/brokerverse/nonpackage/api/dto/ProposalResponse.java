package com.iortatechnxt.brokerverse.nonpackage.api.dto;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * One PRF with its risk details, quotation slip and proposal slip facts.
 *
 * @param id id
 * @param companyId company
 * @param prfNo marketing reference
 * @param arn Account Reference Number
 * @param clientId client
 * @param clientCode client or prospect code
 * @param clientName client name
 * @param clientEmail client e-mail
 * @param productCode product
 * @param lineCode product line
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param currency currency
 * @param periodFrom period start
 * @param periodTo period end
 * @param totalSumInsured total sum insured
 * @param sections free-form sections
 * @param items risk items
 * @param groups risk groups
 * @param insurers insurers requested / selected
 * @param tsuRule TSU routing rule
 * @param tsuReason TSU routing reason
 * @param status status
 * @param slips quotation and proposal slip facts
 * @param acceptedGroups accepted risk groups
 * @param accountArns accounts created
 * @param createdBy maker
 * @param createdAt created
 */
public record ProposalResponse(
    Long id,
    Long companyId,
    String prfNo,
    String arn,
    Long clientId,
    String clientCode,
    String clientName,
    String clientEmail,
    String productCode,
    String lineCode,
    String marketSegment,
    String sourceChannel,
    String currency,
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal totalSumInsured,
    List<RiskDetails.Section> sections,
    List<Item> items,
    List<Integer> groups,
    List<String> insurers,
    String tsuRule,
    String tsuReason,
    ProposalStatus status,
    Slips slips,
    List<Integer> acceptedGroups,
    List<String> accountArns,
    String createdBy,
    Instant createdAt) {

  /**
   * A risk item.
   *
   * @param itemNo position
   * @param riskGroup risk group
   * @param data risk data
   */
  public record Item(int itemNo, int riskGroup, RiskItemData data) {}

  /**
   * Approval, quotation slip and proposal slip facts.
   *
   * @param submittedBy submitted by
   * @param approvedBy approved by (Marketing)
   * @param qsNo quotation slip number
   * @param qsTemplate quotation slip template version
   * @param qsReplyBy reply date asked from the insurers
   * @param qsSubmittedBy quotation slip prepared by
   * @param qsApprovedBy quotation slip approved by
   * @param qsSentAt quotation slip sent
   * @param termsClosed request for terms closed with pending responses
   * @param chosenInsurer chosen insurer
   * @param psNo proposal slip number
   * @param psVersion proposal slip version
   * @param psSubmittedBy proposal slip prepared by
   * @param psApprovedBy proposal slip approved by
   * @param sentAt sent to the client
   * @param acceptedAt accepted by the client
   */
  public record Slips(
      String submittedBy,
      String approvedBy,
      String qsNo,
      String qsTemplate,
      LocalDate qsReplyBy,
      String qsSubmittedBy,
      String qsApprovedBy,
      Instant qsSentAt,
      boolean termsClosed,
      String chosenInsurer,
      String psNo,
      int psVersion,
      String psSubmittedBy,
      String psApprovedBy,
      Instant sentAt,
      Instant acceptedAt) {}

  /**
   * Maps a PRF (collections loaded) with its details.
   *
   * @param p PRF
   * @param details risk details
   * @return response
   */
  public static ProposalResponse from(ProposalRequest p, RiskDetails details) {
    List<RiskDetails.Item> all = details.items();
    return new ProposalResponse(
        p.getId(),
        p.getCompanyId(),
        p.getPrfNo(),
        p.getArn(),
        p.getClientId(),
        p.getClientCode(),
        p.getClientName(),
        p.getClientEmail(),
        p.getProductCode(),
        p.getLineCode(),
        p.getMarketSegment(),
        p.getSourceChannel(),
        p.getCurrency(),
        p.getPeriodFrom(),
        p.getPeriodTo(),
        p.getTotalSumInsured(),
        details.sections(),
        IntStream.range(0, all.size())
            .mapToObj(n -> new Item(n + 1, all.get(n).riskGroup(), all.get(n).data()))
            .toList(),
        details.groups(),
        List.copyOf(p.getInsurers()),
        p.getTsuRule(),
        p.getTsuReason(),
        p.getStatus(),
        new Slips(
            p.getSubmittedBy(),
            p.getApprovedBy(),
            p.getQsNo(),
            p.getQsTemplate(),
            p.getQsReplyBy(),
            p.getQsSubmittedBy(),
            p.getQsApprovedBy(),
            p.getQsSentAt(),
            p.isTermsClosed(),
            p.getChosenInsurer(),
            p.getPsNo(),
            p.getPsVersion(),
            p.getPsSubmittedBy(),
            p.getPsApprovedBy(),
            p.getSentAt(),
            p.getAcceptedAt()),
        p.getAcceptedGroupList(),
        List.copyOf(p.getAccountArns()),
        p.getCreatedBy(),
        p.getCreatedAt());
  }
}
