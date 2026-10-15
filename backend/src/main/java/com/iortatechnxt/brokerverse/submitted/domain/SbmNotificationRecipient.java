package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A recipient of a lifecycle event of submitted policies (FR-SP-082 R3, BRIDSP-28): for an event
 * type and a segment (null for every segment), a role, a named user, the assigned handler or AO of
 * the record, an outside e-mail address of the bank counterpart, or OFF to switch the event off for
 * the segment. Maker and checker.
 */
@Entity
@Table(name = "sbm_notification_recipient")
public class SbmNotificationRecipient extends AuthorizableEntity {

  /** Every holder of a role. */
  public static final String ROLE = "ROLE";

  /** A named user. */
  public static final String USER = "USER";

  /** The assigned handler of the record. */
  public static final String HANDLER = "HANDLER";

  /** The account officer of the record. */
  public static final String AO = "AO";

  /** An outside e-mail address (bank counterpart). */
  public static final String EMAIL = "EMAIL";

  /** The event is not sent for the segment. */
  public static final String OFF = "OFF";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "event_code", nullable = false, length = 40)
  private String eventCode;

  @Column(length = 30)
  private String segment;

  @Column(name = "recipient_type", nullable = false, length = 20)
  private String recipientType;

  @Column(length = 120)
  private String recipient;

  @Column(length = 250)
  private String description;

  protected SbmNotificationRecipient() {}

  /**
   * A recipient.
   *
   * @param companyId company
   * @param row content
   */
  public SbmNotificationRecipient(Long companyId, Row row) {
    this.companyId = companyId;
    set(row);
  }

  /**
   * Changes the recipient (pending approval again).
   *
   * @param row content
   */
  public final void change(Row row) {
    set(row);
    markModified();
  }

  private void set(Row row) {
    this.eventCode = row.eventCode();
    this.segment = row.segment();
    this.recipientType = row.recipientType();
    this.recipient = row.recipient();
    this.description = row.description();
  }

  /**
   * Whether the recipient applies to a record of a segment.
   *
   * @param recordSegment segment of the record
   * @return true for every segment or the record's own
   */
  public boolean appliesTo(String recordSegment) {
    return segment == null || segment.equals(recordSegment);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEventCode() {
    return eventCode;
  }

  public String getSegment() {
    return segment;
  }

  public String getRecipientType() {
    return recipientType;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The content of a recipient.
   *
   * @param eventCode notification event of the module (SBM_*)
   * @param segment segment, null for all
   * @param recipientType ROLE, USER, HANDLER, AO, EMAIL or OFF
   * @param recipient role code, user name or e-mail address; null for HANDLER, AO and OFF
   * @param description description
   */
  public record Row(
      String eventCode,
      String segment,
      String recipientType,
      String recipient,
      String description) {}
}
