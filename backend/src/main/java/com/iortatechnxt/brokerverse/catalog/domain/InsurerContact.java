package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Contact person of an insurer (placement, claims, accounting, remittance), maker-checker. */
@Entity
@Table(name = "cat_insurer_contact")
public class InsurerContact extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "insurer_id", nullable = false, updatable = false)
  private Long insurerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_role", nullable = false, length = 20)
  private ContactRole role;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 80)
  private String position;

  @Column(length = 120)
  private String email;

  @Column(length = 40)
  private String phone;

  protected InsurerContact() {}

  /**
   * Creates a contact, pending authorization.
   *
   * @param insurerId insurer profile
   * @param details contact
   */
  public InsurerContact(Long insurerId, ContactDetails details) {
    this.insurerId = insurerId;
    apply(details);
  }

  /**
   * Changes the contact; it must be authorized again.
   *
   * @param details contact
   */
  public void update(ContactDetails details) {
    apply(details);
    markModified();
  }

  private void apply(ContactDetails d) {
    this.role = d.role();
    this.name = d.name();
    this.position = d.position();
    this.email = d.email();
    this.phone = d.phone();
  }

  @Override
  public String catalogReference() {
    return insurerId + "/" + name;
  }

  @Override
  public String catalogDescription() {
    return name + " (" + role.name() + ")";
  }

  public Long getInsurerId() {
    return insurerId;
  }

  public ContactRole getRole() {
    return role;
  }

  public String getName() {
    return name;
  }

  public String getPosition() {
    return position;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  /** What the contact person handles for the insurer. */
  public enum ContactRole {
    /** Placement and policy issuance. */
    PLACEMENT,
    /** Claims. */
    CLAIMS,
    /** Accounting and statements. */
    ACCOUNTING,
    /** Remittance of premiums. */
    REMITTANCE,
    /** Relationship manager. */
    RELATIONSHIP,
    /** Other. */
    OTHER
  }

  /**
   * Maintainable attributes of a contact.
   *
   * @param role what the contact handles
   * @param name name
   * @param position position
   * @param email e-mail
   * @param phone phone
   */
  public record ContactDetails(
      ContactRole role, String name, String position, String email, String phone) {}
}
