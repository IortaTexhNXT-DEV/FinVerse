package com.iortatechnxt.brokerverse.brokerclaims.location.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An insured location of the cover linked to a claim (BRCLM.037; CLAIMS_BROKING_DESIGN 5.2): the
 * risk item of kind location with a snapshot of its address, city, province and normalised location
 * key, and the damage at that location. A location is linked once per claim; the claim keeps one
 * reference whatever the number of locations (037 AC4).
 */
@Entity
@Table(name = "bcl_claim_location")
public class ClaimLocation extends BaseEntity {

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "account_item_no", nullable = false, updatable = false)
  private int accountItemNo;

  @Column(length = 500, updatable = false)
  private String address;

  @Column(length = 100, updatable = false)
  private String city;

  @Column(length = 100, updatable = false)
  private String province;

  @Column(name = "location_key", length = 400, updatable = false)
  private String locationKey;

  @Column(length = 500)
  private String description;

  protected ClaimLocation() {}

  /**
   * Links a location of the cover.
   *
   * @param claimId claim
   * @param place item number and address snapshot of the cover's location
   * @param description damage at the location, may be null
   */
  public ClaimLocation(Long claimId, Place place, String description) {
    this.claimId = claimId;
    this.accountItemNo = place.itemNo();
    this.address = place.address();
    this.city = place.city();
    this.province = place.province();
    this.locationKey = place.locationKey();
    this.description = description;
  }

  /**
   * Changes the damage description.
   *
   * @param newDescription description
   */
  public void describe(String newDescription) {
    this.description = newDescription;
  }

  public Long getClaimId() {
    return claimId;
  }

  public int getAccountItemNo() {
    return accountItemNo;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getProvince() {
    return province;
  }

  public String getLocationKey() {
    return locationKey;
  }

  public String getDescription() {
    return description;
  }

  /**
   * A location of a cover (risk item of kind location).
   *
   * @param itemNo item number on the account
   * @param address address
   * @param city city
   * @param province province
   * @param locationKey normalised location key
   */
  public record Place(int itemNo, String address, String city, String province, String locationKey) {}
}
