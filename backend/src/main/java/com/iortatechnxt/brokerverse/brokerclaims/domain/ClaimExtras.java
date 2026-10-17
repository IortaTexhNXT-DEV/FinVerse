package com.iortatechnxt.brokerverse.brokerclaims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Details of a claim added with the FRS BRD-07 Claims v1.0 (BRD scope): the plate number of a motor
 * claim (FRCLM.008.02, 009.01), the preferred repair shop, a party of type GARAGE (FRCLM.007.02),
 * and the return of the claim of an unpaid policy to the Account Officer (FRCLM.008.04).
 */
@Embeddable
public class ClaimExtras {

  /** Longest note of a return to the Account Officer. */
  public static final int MAX_RETURN_NOTE = 500;

  @Column(name = "plate_no", length = 20)
  private String plateNo;

  @Column(name = "preferred_shop_code", length = 30)
  private String preferredShopCode;

  @Column(name = "returned_to_ao_at")
  private Instant returnedToAoAt;

  @Column(name = "returned_to_ao_by", length = 50)
  private String returnedToAoBy;

  @Column(name = "returned_to_ao_note", length = MAX_RETURN_NOTE)
  private String returnedToAoNote;

  /**
   * Sets the plate number and the preferred repair shop.
   *
   * @param plate plate number, may be null
   * @param shopCode repair shop party code, may be null
   */
  public void motor(String plate, String shopCode) {
    this.plateNo = plate;
    this.preferredShopCode = shopCode;
  }

  /**
   * Records the return of the claim to the Account Officer for the collection of the premium.
   *
   * @param by user
   * @param at time
   * @param note note to the Account Officer
   */
  public void returnedToAccountOfficer(String by, Instant at, String note) {
    this.returnedToAoBy = by;
    this.returnedToAoAt = at;
    this.returnedToAoNote = note;
  }

  public String getPlateNo() {
    return plateNo;
  }

  public String getPreferredShopCode() {
    return preferredShopCode;
  }

  public Instant getReturnedToAoAt() {
    return returnedToAoAt;
  }

  public String getReturnedToAoBy() {
    return returnedToAoBy;
  }

  public String getReturnedToAoNote() {
    return returnedToAoNote;
  }
}
