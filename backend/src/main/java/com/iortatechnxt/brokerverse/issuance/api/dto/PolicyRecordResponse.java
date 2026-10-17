package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService.PolicyRecord;
import java.time.LocalDate;
import java.util.List;

/**
 * The policy record of an account: policy numbers, issue date, e-policies and Insurance Advices.
 *
 * @param arn Account Reference Number
 * @param status account status
 * @param policyNumbers policy numbers
 * @param issueDate issue date
 * @param epolicies e-policies, newest first
 * @param advices Insurance Advices, newest first
 */
public record PolicyRecordResponse(
    String arn,
    String status,
    List<String> policyNumbers,
    LocalDate issueDate,
    List<EpolicyResponse> epolicies,
    List<AdviceResponse> advices) {

  /**
   * Maps a policy record.
   *
   * @param p record
   * @return response
   */
  public static PolicyRecordResponse from(PolicyRecord p) {
    return new PolicyRecordResponse(
        p.arn(),
        p.status().name(),
        p.policyNumbers(),
        p.issueDate(),
        p.epolicies().stream().map(EpolicyResponse::from).toList(),
        p.advices().stream().map(AdviceResponse::from).toList());
  }
}
