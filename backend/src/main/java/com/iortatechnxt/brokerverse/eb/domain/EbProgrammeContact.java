package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A client contact of a programme (design 4.2): HR head, HR officer or finance, with the flags for
 * the renewal advice and SOA e-mails (BRID-001, 021).
 */
@Entity
@Table(name = "eb_programme_contact")
public class EbProgrammeContact extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "programme_id", nullable = false, updatable = false)
  private EbProgramme programme;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 120)
  private String email;

  @Column(length = 30)
  private String mobile;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbContactRole role;

  @Column(name = "receives_ra", nullable = false)
  private boolean receivesRa;

  @Column(name = "receives_soa", nullable = false)
  private boolean receivesSoa;

  @Column(nullable = false)
  private boolean active = true;

  protected EbProgrammeContact() {}

  EbProgrammeContact(EbProgramme programme, Data data) {
    this.programme = programme;
    apply(data);
  }

  /**
   * Replaces the data of the contact.
   *
   * @param data contact data
   */
  public void update(Data data) {
    apply(data);
  }

  private void apply(Data data) {
    this.name = data.name();
    this.email = data.email();
    this.mobile = data.mobile();
    this.role = data.role();
    this.receivesRa = data.receivesRa();
    this.receivesSoa = data.receivesSoa();
  }

  /** The contact no longer receives anything (kept for history). */
  public void deactivate() {
    this.active = false;
  }

  public EbProgramme getProgramme() {
    return programme;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getMobile() {
    return mobile;
  }

  public EbContactRole getRole() {
    return role;
  }

  public boolean isReceivesRa() {
    return receivesRa;
  }

  public boolean isReceivesSoa() {
    return receivesSoa;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Data of a contact.
   *
   * @param name full name
   * @param email e-mail address
   * @param mobile mobile number
   * @param role HR head, HR officer or finance
   * @param receivesRa receives the renewal advice and reminders
   * @param receivesSoa receives the released SOAs
   */
  public record Data(
      String name,
      String email,
      String mobile,
      EbContactRole role,
      boolean receivesRa,
      boolean receivesSoa) {}
}
