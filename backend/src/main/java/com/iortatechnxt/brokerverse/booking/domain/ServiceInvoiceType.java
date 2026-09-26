package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Service invoice type (BRNB.100): recipient, trigger (on booking, on endorsement, manual), owner
 * (a team through a permission, or a user) notified of the dispatch, and the document template.
 */
@Entity
@Table(name = "bkg_si_type")
public class ServiceInvoiceType extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SiRecipient recipient;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20)
  private SiTrigger trigger;

  @Column(name = "owner_permission", length = 60)
  private String ownerPermission;

  @Column(name = "owner_username", length = 50)
  private String ownerUsername;

  @Column(name = "template_code", nullable = false, length = 40)
  private String templateCode;

  @Column(nullable = false)
  private boolean active;

  protected ServiceInvoiceType() {}

  /**
   * Creates a type.
   *
   * @param code code
   * @param settings settings
   */
  public ServiceInvoiceType(String code, Settings settings) {
    this.code = code;
    apply(settings);
  }

  /**
   * Replaces the settings.
   *
   * @param settings settings
   */
  public final void apply(Settings settings) {
    this.name = settings.name();
    this.recipient = settings.recipient();
    this.trigger = settings.trigger();
    this.ownerPermission = blankToNull(settings.ownerPermission());
    this.ownerUsername = blankToNull(settings.ownerUsername());
    this.templateCode = settings.templateCode();
    this.active = settings.active();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public SiRecipient getRecipient() {
    return recipient;
  }

  public SiTrigger getTrigger() {
    return trigger;
  }

  public String getOwnerPermission() {
    return ownerPermission;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Type settings.
   *
   * @param name name
   * @param recipient recipient
   * @param trigger trigger
   * @param ownerPermission owning team (permission whose holders are notified), may be blank
   * @param ownerUsername owning user, may be blank
   * @param templateCode document template (docgen)
   * @param active active
   */
  public record Settings(
      String name,
      SiRecipient recipient,
      SiTrigger trigger,
      String ownerPermission,
      String ownerUsername,
      String templateCode,
      boolean active) {}
}
