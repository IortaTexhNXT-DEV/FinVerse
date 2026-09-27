package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import java.math.BigDecimal;

/**
 * A risk item of an account.
 *
 * @param id id
 * @param itemNo item number
 * @param kind item kind
 * @param label label (plate, address, person or description)
 * @param description description
 * @param sumInsured sum insured
 * @param rate rate %
 * @param premium annual premium
 * @param biLimit motor excess BI limit
 * @param pdLimit motor PD limit
 * @param vehicle vehicle block (vehicles)
 * @param location location block (property)
 * @param person person block (personal accident)
 */
public record ItemResponse(
    Long id,
    int itemNo,
    RiskItemKind kind,
    String label,
    String description,
    BigDecimal sumInsured,
    BigDecimal rate,
    BigDecimal premium,
    BigDecimal biLimit,
    BigDecimal pdLimit,
    Vehicle vehicle,
    Location location,
    Person person) {

  /**
   * Maps an item.
   *
   * @param i item
   * @return response
   */
  public static ItemResponse from(RiskItem i) {
    return new ItemResponse(
        i.getId(),
        i.getItemNo(),
        i.getKind(),
        i.label(),
        i.getDescription(),
        i.getSumInsured(),
        i.getRate(),
        i.getPremium(),
        i.getBiLimit(),
        i.getPdLimit(),
        i.getKind() == RiskItemKind.VEHICLE ? i.vehicle() : null,
        i.getKind() == RiskItemKind.PROPERTY_LOCATION ? i.location() : null,
        i.getKind() == RiskItemKind.PERSON ? i.person() : null);
  }
}
