package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A legacy client registered by the data migration (DATA_MIGRATION_DESIGN section 9): its data and
 * KYC profile, the KYC status and dates kept from legacy, and where it comes from.
 *
 * @param companyId company
 * @param details client data
 * @param profile KYC profile
 * @param kyc KYC status, verification time and next review kept from legacy
 * @param origin source system, legacy client code and migration batch
 */
public record MigratedClient(
    Long companyId, ClientDetails details, ClientProfile profile, Kyc kyc, RecordOrigin origin) {

  /**
   * The KYC of a legacy client.
   *
   * @param status KYC status
   * @param verifiedAt verification time, null when not verified
   * @param reviewDue next periodic review, null when unknown
   */
  public record Kyc(KycStatus status, Instant verifiedAt, LocalDate reviewDue) {

    /** No KYC recorded in legacy. */
    public static final Kyc NONE = new Kyc(KycStatus.NOT_STARTED, null, null);
  }
}
