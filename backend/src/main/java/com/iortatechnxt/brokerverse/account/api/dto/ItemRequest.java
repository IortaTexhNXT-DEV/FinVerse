package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A risk item as entered (BRNB.051); only the block of the product line's item kind is kept.
 *
 * @param description description
 * @param sumInsured sum insured
 * @param rate premium rate %, empty for the product default
 * @param biLimit motor excess BI limit
 * @param pdLimit motor PD limit
 * @param vehicle vehicle block
 * @param location location block
 * @param person person block
 */
public record ItemRequest(
    @Size(max = 300) String description,
    @DecimalMin("0") BigDecimal sumInsured,
    @DecimalMin("0") @DecimalMax("100") BigDecimal rate,
    @DecimalMin("0") BigDecimal biLimit,
    @DecimalMin("0") BigDecimal pdLimit,
    @Valid VehicleBlock vehicle,
    @Valid LocationBlock location,
    @Valid PersonBlock person) {

  /**
   * Vehicle.
   *
   * @param plateNo plate number
   * @param conductionSticker conduction sticker
   * @param engineNo engine number
   * @param chassisNo chassis / serial number
   * @param make make
   * @param model model
   * @param yearModel year model
   * @param bodyType body type
   * @param colour colour
   * @param seatingCapacity seats
   */
  public record VehicleBlock(
      @Size(max = 20) String plateNo,
      @Size(max = 20) String conductionSticker,
      @Size(max = 40) String engineNo,
      @Size(max = 40) String chassisNo,
      @Size(max = 60) String make,
      @Size(max = 60) String model,
      @Min(1900) @Max(2100) Integer yearModel,
      @Size(max = 40) String bodyType,
      @Size(max = 40) String colour,
      @Min(1) @Max(100) Integer seatingCapacity) {}

  /**
   * Location of risk.
   *
   * @param address full address
   * @param city city
   * @param province province
   * @param occupancy occupancy
   * @param constructionClass construction class
   * @param insuredItems items insured
   */
  public record LocationBlock(
      @Size(max = 300) String address,
      @Size(max = 80) String city,
      @Size(max = 80) String province,
      @Size(max = 40) String occupancy,
      @Size(max = 40) String constructionClass,
      @Size(max = 50) List<@Valid InsuredItemBlock> insuredItems) {}

  /**
   * Item insured at a location.
   *
   * @param description item
   * @param sumInsured sum insured
   */
  public record InsuredItemBlock(
      @NotBlank @Size(max = 200) String description,
      @NotNull @DecimalMin("0") BigDecimal sumInsured) {}

  /**
   * Insured person.
   *
   * @param name name
   * @param birthDate birth date
   * @param relationship relationship
   */
  public record PersonBlock(
      @Size(max = 200) String name, LocalDate birthDate, @Size(max = 40) String relationship) {}

  /**
   * As item data.
   *
   * @return data
   */
  public RiskItemData data() {
    return new RiskItemData(
        blankToNull(description),
        sumInsured,
        rate,
        biLimit,
        pdLimit,
        vehicle == null
            ? null
            : new Vehicle(
                blankToNull(vehicle.plateNo()),
                blankToNull(vehicle.conductionSticker()),
                blankToNull(vehicle.engineNo()),
                blankToNull(vehicle.chassisNo()),
                blankToNull(vehicle.make()),
                blankToNull(vehicle.model()),
                vehicle.yearModel(),
                blankToNull(vehicle.bodyType()),
                blankToNull(vehicle.colour()),
                vehicle.seatingCapacity()),
        location == null
            ? null
            : new Location(
                blankToNull(location.address()),
                blankToNull(location.city()),
                blankToNull(location.province()),
                blankToNull(location.occupancy()),
                blankToNull(location.constructionClass()),
                location.insuredItems() == null
                    ? List.of()
                    : location.insuredItems().stream()
                        .map(i -> new InsuredItem(i.description().strip(), i.sumInsured()))
                        .toList()),
        person == null
            ? null
            : new Person(
                blankToNull(person.name()),
                person.birthDate(),
                blankToNull(person.relationship())));
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
