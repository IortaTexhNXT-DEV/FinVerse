package com.iortatechnxt.brokerverse.renewal.candidate.api;

import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Codes;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.report.core.CodeSet;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the renewal lists. Each multi-select criterion is a comma list of codes, or
 * "!" followed by the codes to exclude ("all except").
 *
 * @param companyId company
 * @param tab tab (default ALL)
 * @param q search text
 * @param expiryFrom first expiry
 * @param expiryTo last expiry
 * @param unitHead unit heads
 * @param origin business origins
 * @param accountType account types
 * @param region regions
 * @param department departments
 * @param branch branches
 * @param riskCode risk codes
 * @param segment segments
 * @param officer account officers
 * @param bucket buckets
 * @param disposition dispositions
 * @param stage stages
 * @param insurer insurers
 * @param mine renewals ever assigned to the user
 * @param assignedPo Processing Officer
 * @param returned returned only
 * @param nrns NRNS only
 * @param urgent urgent only
 * @param kycDue KYC due only
 * @param dueWithin expiring within the days
 * @param disposed dispositioned only
 */
public record CandidateListParams(
    Long companyId,
    Tab tab,
    String q,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
    String unitHead,
    String origin,
    String accountType,
    String region,
    String department,
    String branch,
    String riskCode,
    String segment,
    String officer,
    String bucket,
    String disposition,
    String stage,
    String insurer,
    Boolean mine,
    String assignedPo,
    Boolean returned,
    Boolean nrns,
    Boolean urgent,
    Boolean kycDue,
    Integer dueWithin,
    Boolean disposed) {

  /**
   * The list criteria.
   *
   * @return filter
   */
  public CandidateFilter filter() {
    return new CandidateFilter(
        companyId,
        tab == null ? Tab.ALL : tab,
        q,
        expiryFrom,
        expiryTo,
        new Codes(
            CodeSet.parse(unitHead),
            CodeSet.parse(origin),
            CodeSet.parse(accountType),
            CodeSet.parse(region),
            CodeSet.parse(department),
            CodeSet.parse(branch),
            CodeSet.parse(riskCode),
            CodeSet.parse(segment),
            CodeSet.parse(officer),
            CodeSet.parse(bucket),
            CodeSet.parse(disposition),
            CodeSet.parse(stage),
            CodeSet.parse(insurer)),
        new Flags(
            Boolean.TRUE.equals(mine),
            assignedPo,
            Boolean.TRUE.equals(returned),
            Boolean.TRUE.equals(nrns),
            Boolean.TRUE.equals(urgent),
            Boolean.TRUE.equals(kycDue),
            dueWithin,
            Boolean.TRUE.equals(disposed)));
  }
}
