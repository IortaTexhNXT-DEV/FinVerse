package com.iortatechnxt.brokerverse.brokerclaims.location.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.location.domain.ClaimLocation;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService.NewRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the claim locations and insurer location references. */
public final class LocationDtos {

  private LocationDtos() {}

  /**
   * An insurer location reference (BRCLM.042).
   *
   * @param id id
   * @param arn cover
   * @param itemNo location item number
   * @param locationKey normalised location key
   * @param insurerCode insurer
   * @param reference insurer's location reference
   * @param effectiveFrom first day of validity
   * @param effectiveTo last day, null while open
   * @param createdBy user
   * @param createdAt time
   */
  public record LocationRefResponse(
      Long id,
      String arn,
      int itemNo,
      String locationKey,
      String insurerCode,
      String reference,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a reference.
     *
     * @param r reference
     * @return response
     */
    public static LocationRefResponse from(LocationRef r) {
      return new LocationRefResponse(
          r.getId(),
          r.getArn(),
          r.getAccountItemNo(),
          r.getLocationKey(),
          r.getInsurerCode(),
          r.getInsurerLocationRef(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }

  /**
   * A location of a claim with the insurer references valid today (BRCLM.037/042).
   *
   * @param itemNo item number on the cover
   * @param address address
   * @param city city
   * @param province province
   * @param locationKey normalised location key
   * @param description damage at the location
   * @param references insurer references valid today
   */
  public record ClaimLocationResponse(
      int itemNo,
      String address,
      String city,
      String province,
      String locationKey,
      String description,
      List<LocationRefResponse> references) {

    /**
     * Maps a location with its references.
     *
     * @param l location
     * @param refs references of the cover valid today
     * @return response
     */
    public static ClaimLocationResponse from(ClaimLocation l, List<LocationRef> refs) {
      return new ClaimLocationResponse(
          l.getAccountItemNo(),
          l.getAddress(),
          l.getCity(),
          l.getProvince(),
          l.getLocationKey(),
          l.getDescription(),
          refs.stream()
              .filter(r -> r.getAccountItemNo() == l.getAccountItemNo())
              .map(LocationRefResponse::from)
              .toList());
    }
  }

  /**
   * A location of the cover to link.
   *
   * @param itemNo item number
   * @param description damage, may be empty
   */
  public record LocationPickRequest(int itemNo, @Size(max = 500) String description) {

    /**
     * The service input.
     *
     * @return pick
     */
    public LocationPick toPick() {
      return new LocationPick(itemNo, description);
    }
  }

  /**
   * Locations to link to a claim.
   *
   * @param locations locations of the cover
   */
  public record LinkRequest(@NotEmpty List<@Valid LocationPickRequest> locations) {}

  /**
   * A damage description.
   *
   * @param description description
   */
  public record DescribeRequest(@Size(max = 500) String description) {}

  /**
   * A reference to record.
   *
   * @param companyId company
   * @param arn cover
   * @param itemNo location item
   * @param insurerCode insurer
   * @param reference insurer's location reference
   * @param effectiveFrom first day, today when empty
   */
  public record LocationRefRequest(
      Long companyId,
      @NotBlank String arn,
      int itemNo,
      String insurerCode,
      @Size(max = 60) String reference,
      LocalDate effectiveFrom) {

    /**
     * The service input.
     *
     * @return reference
     */
    public NewRef toRef() {
      return new NewRef(arn, itemNo, insurerCode, reference, effectiveFrom);
    }
  }
}
