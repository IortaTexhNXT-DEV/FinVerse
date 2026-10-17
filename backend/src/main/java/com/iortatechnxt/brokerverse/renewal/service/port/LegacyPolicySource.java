package com.iortatechnxt.brokerverse.renewal.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port of Renewal for the policies booked in the legacy systems (EBIX / QPS) before go-live (RQ27;
 * DATA_MIGRATION_DESIGN section 15). Implemented by Data Migration ({@code MigratedPolicySource})
 * from the migrated in-force policy headers (object P01) and the Renewal Advices already sent by
 * hand (object P03). Until it is delivered the default adapter answers "not connected" and the bulk
 * upload {@code RNW_LEGACY_POLICIES} is the fallback.
 *
 * <p>BDOI answered DMQ37 on 26-Sep-2026: no renewal candidate is carried from legacy. At go-live
 * Renewal takes over every migrated header expiring from go-live to {@code MIG_GOLIVE_RENEWAL_TO}
 * ({@link #goLiveCandidates}), the January expiries flagged urgent up to {@code
 * MIG_RENEWAL_URGENT_TO}, and the daily extraction later asks for the headers of its date ({@link
 * #expiringHeaders}); a header that already has a candidate is skipped.
 */
public interface LegacyPolicySource {

  /**
   * Whether a real source is connected.
   *
   * @return false for the default adapter
   */
  boolean connected();

  /**
   * The migrated in-force headers expiring in a range (daily extraction, section 15 "from
   * 1-Jun-2028").
   *
   * @param companyId company
   * @param from first expiry date
   * @param to last expiry date
   * @return headers, earliest expiry first
   */
  List<LegacyHeader> expiringHeaders(Long companyId, LocalDate from, LocalDate to);

  /**
   * The go-live take-over (DMQ37): every migrated header expiring from go-live to the end of the
   * window that was not renewed in legacy, with the urgent flag and the Renewal Advice already sent
   * (P03), and the number of headers skipped because a later term of the same cover was migrated.
   *
   * @param companyId company
   * @param goLive go-live date (first expiry of the window)
   * @param to last expiry of the window
   * @return candidates and the count of headers renewed in legacy
   */
  GoLiveHeaders goLiveCandidates(Long companyId, LocalDate goLive, LocalDate to);

  /**
   * A migrated in-force policy header (P01).
   *
   * @param legacyRef legacy policy reference (the key of the candidate)
   * @param sourceSystem EBIX or QPS
   * @param arn ARN of the migrated account, null when none
   * @param policy policy facts
   * @param parties client, insurer, officer and unit
   * @param urgent expiring up to the urgent date of the go-live window
   * @param raSent Renewal Advice already sent by hand before go-live, null when none
   */
  record LegacyHeader(
      String legacyRef,
      String sourceSystem,
      String arn,
      LegacyPolicy policy,
      LegacyParties parties,
      boolean urgent,
      LegacyRaSent raSent) {}

  /**
   * Policy facts of a header.
   *
   * @param policyNo policy number
   * @param coverNo cover number
   * @param productCode BIBS risk code, null when not mapped
   * @param lineCode product line
   * @param legacyPackageCode legacy package code as stored in legacy (not remapped, DMQ36)
   * @param legacyPackageVersion legacy package version
   * @param inceptionDate period start
   * @param expiryDate period end
   * @param sumInsured total sum insured
   * @param grossPremium gross premium
   * @param currency currency
   * @param pnNos PN numbers, comma separated
   */
  record LegacyPolicy(
      String policyNo,
      String coverNo,
      String productCode,
      String lineCode,
      String legacyPackageCode,
      String legacyPackageVersion,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      BigDecimal sumInsured,
      BigDecimal grossPremium,
      String currency,
      String pnNos) {}

  /**
   * Parties of a header.
   *
   * @param clientCode BIBS client code (cross-reference)
   * @param clientName client name
   * @param assuredName assured name
   * @param insurerCode insurer party code
   * @param accountOfficer account officer user name
   * @param salesUnit sales team code
   * @param segment market segment
   * @param mortgageeBank mortgagee bank, null when not mortgaged
   */
  record LegacyParties(
      String clientCode,
      String clientName,
      String assuredName,
      String insurerCode,
      String accountOfficer,
      String salesUnit,
      String segment,
      String mortgageeBank) {}

  /**
   * A Renewal Advice sent by hand before go-live (P03).
   *
   * @param sentOn date the RA was sent
   * @param reference RA reference of the tracker
   * @param channel e-mail, courier, hand or other
   * @param recipient recipient
   */
  record LegacyRaSent(LocalDate sentOn, String reference, String channel, String recipient) {}

  /**
   * The answer of the go-live take-over.
   *
   * @param headers headers to take over
   * @param renewedInLegacy headers skipped because a later term of the cover was migrated
   */
  record GoLiveHeaders(List<LegacyHeader> headers, int renewedInLegacy) {

    /** Defensive copy. */
    public GoLiveHeaders {
      headers = List.copyOf(headers);
    }
  }
}
