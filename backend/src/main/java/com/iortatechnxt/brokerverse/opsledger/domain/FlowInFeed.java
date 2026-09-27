package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A data feed between BrokerVerse and another system (BRQID.004): partner, direction, transport,
 * owner module, schedule and whether it is active. Seeded in V761; each Operations module owns its
 * feeds.
 */
@Entity
@Table(name = "ops_flow_in_feed")
public class FlowInFeed extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "partner_system", nullable = false, length = 30)
  private String partnerSystem;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private FlowInEnums.Direction direction;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FlowInEnums.Transport transport;

  @Column(name = "owner_module", nullable = false, length = 30)
  private String ownerModule;

  @Column(nullable = false, length = 60)
  private String cron;

  @Column(nullable = false)
  private boolean active;

  @Column(length = 300)
  private String description;

  protected FlowInFeed() {}

  /**
   * Changes the schedule and activation (interface administration, FLOWIN_MANAGE).
   *
   * @param newCron Spring cron or "-" for manual only
   * @param isActive whether runs are accepted
   */
  public void configure(String newCron, boolean isActive) {
    this.cron = newCron == null || newCron.isBlank() ? "-" : newCron.strip();
    this.active = isActive;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getPartnerSystem() {
    return partnerSystem;
  }

  public FlowInEnums.Direction getDirection() {
    return direction;
  }

  public FlowInEnums.Transport getTransport() {
    return transport;
  }

  public String getOwnerModule() {
    return ownerModule;
  }

  public String getCron() {
    return cron;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }
}
