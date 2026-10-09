package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.EntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The payor of a creation record: entry type with the client or insurer, the insurer's bank and the
 * payor name (FRS.CSH.02.01.02, 02.02.02).
 *
 * @param entryType client, insurer or other
 * @param clientCode client code, may be null
 * @param clientName client name, may be null
 * @param insurerCode insurer code, may be null
 * @param insurerName insurer name, may be null
 * @param insurerBank bank of the insurer banking list, may be null
 * @param payorName payor name
 */
@Embeddable
public record RecordParty(
    @Enumerated(EnumType.STRING) @Column(name = "entry_type", length = 10) EntryType entryType,
    @Column(name = "client_code", length = 30) String clientCode,
    @Column(name = "client_name", length = 250) String clientName,
    @Column(name = "insurer_code", length = 30) String insurerCode,
    @Column(name = "insurer_name", length = 250) String insurerName,
    @Column(name = "insurer_bank", length = 100) String insurerBank,
    @Column(name = "payor_name", length = 250) String payorName) {

  /**
   * The code of the payor party: the insurer for an insurer entry, else the client.
   *
   * @return party code, may be null
   */
  public String partyCode() {
    return entryType == EntryType.INSURER ? insurerCode : clientCode;
  }
}
