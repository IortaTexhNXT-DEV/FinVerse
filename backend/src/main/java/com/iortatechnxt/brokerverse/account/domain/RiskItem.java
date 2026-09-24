package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One insured item of an account (BRNB.051): a vehicle, a location of risk with its items insured,
 * an insured person or a generic risk, with its sum insured, rate and premium. Vehicle identifiers
 * are stored normalised ({@link RiskIdentifiers}) for the duplicate fall-out.
 */
@Entity
@Table(name = "acc_risk_item")
public class RiskItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false, updatable = false)
  private Account account;

  @Column(name = "item_no", nullable = false, updatable = false)
  private int itemNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RiskItemKind kind;

  @Column(length = 300)
  private String description;

  @Column(name = "sum_insured", precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(name = "bi_limit", precision = 19, scale = 2)
  private BigDecimal biLimit;

  @Column(name = "pd_limit", precision = 19, scale = 2)
  private BigDecimal pdLimit;

  @Column(name = "plate_no", length = 20)
  private String plateNo;

  @Column(name = "conduction_sticker", length = 20)
  private String conductionSticker;

  @Column(name = "engine_no", length = 40)
  private String engineNo;

  @Column(name = "chassis_no", length = 40)
  private String chassisNo;

  @Column(length = 60)
  private String make;

  @Column(length = 60)
  private String model;

  @Column(name = "year_model")
  private Integer yearModel;

  @Column(name = "body_type", length = 40)
  private String bodyType;

  @Column(length = 40)
  private String colour;

  @Column(name = "seating_capacity")
  private Integer seatingCapacity;

  @Column(length = 300)
  private String address;

  @Column(length = 80)
  private String city;

  @Column(length = 80)
  private String province;

  @Column(length = 40)
  private String occupancy;

  @Column(name = "construction_class", length = 40)
  private String constructionClass;

  @Column(name = "location_key", length = 400)
  private String locationKey;

  @ElementCollection
  @CollectionTable(name = "acc_risk_item_detail", joinColumns = @JoinColumn(name = "risk_item_id"))
  @OrderColumn(name = "detail_index")
  private final List<InsuredItem> insuredItems = new ArrayList<>();

  @Column(name = "person_name", length = 200)
  private String personName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 40)
  private String relationship;

  protected RiskItem() {}

  RiskItem(Account account, int itemNo, RiskItemKind kind, RiskItemData data) {
    this.account = account;
    this.itemNo = itemNo;
    update(kind, data);
  }

  final void update(RiskItemKind newKind, RiskItemData data) {
    this.kind = newKind;
    this.description = data.description();
    this.rate = data.rate();
    this.biLimit = data.biLimit();
    this.pdLimit = data.pdLimit();
    this.premium = null;
    applyVehicle(newKind == RiskItemKind.VEHICLE ? data.vehicle() : null);
    applyLocation(newKind == RiskItemKind.PROPERTY_LOCATION ? data.location() : null);
    applyPerson(newKind == RiskItemKind.PERSON ? data.person() : null);
    this.sumInsured = insuredItems.isEmpty() ? data.sumInsured() : insuredTotal();
  }

  private void applyVehicle(Vehicle v) {
    Vehicle value =
        v == null ? new Vehicle(null, null, null, null, null, null, null, null, null, null) : v;
    this.plateNo = RiskIdentifiers.vehicleId(value.plateNo());
    this.conductionSticker = RiskIdentifiers.vehicleId(value.conductionSticker());
    this.engineNo = RiskIdentifiers.vehicleId(value.engineNo());
    this.chassisNo = RiskIdentifiers.vehicleId(value.chassisNo());
    this.make = value.make();
    this.model = value.model();
    this.yearModel = value.yearModel();
    this.bodyType = value.bodyType();
    this.colour = value.colour();
    this.seatingCapacity = value.seatingCapacity();
  }

  private void applyLocation(Location l) {
    Location value = l == null ? new Location(null, null, null, null, null, null) : l;
    this.address = value.address();
    this.city = value.city();
    this.province = value.province();
    this.occupancy = value.occupancy();
    this.constructionClass = value.constructionClass();
    this.locationKey = RiskIdentifiers.locationKey(value.address(), value.city());
    this.insuredItems.clear();
    this.insuredItems.addAll(value.insuredItems());
  }

  private void applyPerson(Person p) {
    this.personName = p == null ? null : p.name();
    this.birthDate = p == null ? null : p.birthDate();
    this.relationship = p == null ? null : p.relationship();
  }

  private BigDecimal insuredTotal() {
    return insuredItems.stream()
        .map(InsuredItem::sumInsured)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Records the premium computed for the item.
   *
   * @param newPremium annual premium, null when not rated
   * @param appliedRate rate used in percent
   */
  public void rated(BigDecimal newPremium, BigDecimal appliedRate) {
    this.premium = newPremium;
    if (appliedRate != null) {
      this.rate = appliedRate;
    }
  }

  /**
   * The vehicle block.
   *
   * @return vehicle
   */
  public Vehicle vehicle() {
    return new Vehicle(
        plateNo,
        conductionSticker,
        engineNo,
        chassisNo,
        make,
        model,
        yearModel,
        bodyType,
        colour,
        seatingCapacity);
  }

  /**
   * The location block.
   *
   * @return location
   */
  public Location location() {
    return new Location(address, city, province, occupancy, constructionClass, insuredItems);
  }

  /**
   * The person block.
   *
   * @return person
   */
  public Person person() {
    return new Person(personName, birthDate, relationship);
  }

  /**
   * Label for lists and rating: plate, address, person or description.
   *
   * @return label
   */
  public String label() {
    return switch (kind) {
      case VEHICLE -> firstNonNull(plateNo, conductionSticker, chassisNo, description);
      case PROPERTY_LOCATION -> firstNonNull(address, description, null, null);
      case PERSON -> firstNonNull(personName, description, null, null);
      case GENERIC -> firstNonNull(description, null, null, null);
    };
  }

  private String firstNonNull(String a, String b, String c, String d) {
    for (String value : new String[] {a, b, c, d}) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "Item " + itemNo;
  }

  public Account getAccount() {
    return account;
  }

  public int getItemNo() {
    return itemNo;
  }

  public RiskItemKind getKind() {
    return kind;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getBiLimit() {
    return biLimit;
  }

  public BigDecimal getPdLimit() {
    return pdLimit;
  }

  public String getPlateNo() {
    return plateNo;
  }

  public String getConductionSticker() {
    return conductionSticker;
  }

  public String getEngineNo() {
    return engineNo;
  }

  public String getChassisNo() {
    return chassisNo;
  }

  public String getLocationKey() {
    return locationKey;
  }

  public List<InsuredItem> getInsuredItems() {
    return List.copyOf(insuredItems);
  }
}
