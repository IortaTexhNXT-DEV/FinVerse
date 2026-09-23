package com.iortatechnxt.finverse.currency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** ISO 4217 currency master. The ISO code is the natural primary key. */
@Entity
@Table(name = "cur_currency")
public class Currency {

  @Id
  @Column(length = 3)
  private String code;

  @Column(nullable = false, length = 60)
  private String name;

  @Column(length = 5)
  private String symbol;

  @Column(name = "decimal_places", nullable = false)
  private int decimalPlaces = 2;

  @Column(nullable = false)
  private boolean active = true;

  protected Currency() {}

  /**
   * Creates a currency.
   *
   * @param code ISO code
   * @param name name
   * @param symbol symbol
   */
  public Currency(String code, String name, String symbol) {
    this.code = code;
    this.name = name;
    this.symbol = symbol;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public int getDecimalPlaces() {
    return decimalPlaces;
  }

  public void setDecimalPlaces(int decimalPlaces) {
    this.decimalPlaces = decimalPlaces;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
