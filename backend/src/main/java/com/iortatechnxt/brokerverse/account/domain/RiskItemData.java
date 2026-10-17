package com.iortatechnxt.brokerverse.account.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data of one risk item (BRNB.051). Only the block of the item's kind is used: vehicle, location or
 * person; generic items carry a description and sum insured.
 *
 * @param description description (generic risks, project, cargo...)
 * @param sumInsured sum insured (for a location: the sum of its items insured when given)
 * @param rate premium rate in percent, null for the product default
 * @param biLimit motor excess bodily injury limit
 * @param pdLimit motor property damage limit
 * @param vehicle vehicle block
 * @param location location of risk block
 * @param person insured person block
 */
public record RiskItemData(
    String description,
    BigDecimal sumInsured,
    BigDecimal rate,
    BigDecimal biLimit,
    BigDecimal pdLimit,
    Vehicle vehicle,
    Location location,
    Person person) {

  /**
   * A generic item.
   *
   * @param description description
   * @param sumInsured sum insured
   * @param rate rate in percent
   * @return item
   */
  public static RiskItemData generic(String description, BigDecimal sumInsured, BigDecimal rate) {
    return new RiskItemData(description, sumInsured, rate, null, null, null, null, null);
  }

  /**
   * Motor vehicle.
   *
   * @param plateNo plate number
   * @param conductionSticker conduction sticker (new vehicle without plate)
   * @param engineNo engine / motor number
   * @param chassisNo chassis / serial number
   * @param make make
   * @param model model
   * @param yearModel year model
   * @param bodyType body type (list VEHICLE_BODY_TYPE)
   * @param colour colour
   * @param seatingCapacity seating capacity
   */
  public record Vehicle(
      String plateNo,
      String conductionSticker,
      String engineNo,
      String chassisNo,
      String make,
      String model,
      Integer yearModel,
      String bodyType,
      String colour,
      Integer seatingCapacity) {}

  /**
   * Location of risk.
   *
   * @param address full address of risk
   * @param city city / municipality
   * @param province province
   * @param occupancy occupancy (list OCCUPANCY)
   * @param constructionClass construction class (list CONSTRUCTION_CLASS)
   * @param insuredItems items insured with their sums insured
   */
  public record Location(
      String address,
      String city,
      String province,
      String occupancy,
      String constructionClass,
      List<InsuredItem> insuredItems) {

    /** Defensive copy. */
    public Location {
      insuredItems = insuredItems == null ? List.of() : List.copyOf(insuredItems);
    }
  }

  /**
   * Insured person (personal accident).
   *
   * @param name full name
   * @param birthDate birth date
   * @param relationship relationship to the client
   */
  public record Person(String name, LocalDate birthDate, String relationship) {}
}
