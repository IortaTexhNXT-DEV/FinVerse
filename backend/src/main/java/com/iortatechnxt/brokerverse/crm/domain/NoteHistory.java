package com.iortatechnxt.brokerverse.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Insert-only history of a client tag or instruction change: who, when, from and to (BRNB.091). */
@Entity
@Table(name = "crm_client_note_history")
public class NoteHistory {

  private static final int MAX_VALUE = 1200;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "item_kind", nullable = false, length = 20, updatable = false)
  private String itemKind;

  @Column(name = "item_ref", nullable = false, length = 60, updatable = false)
  private String itemRef;

  @Column(nullable = false, length = 20, updatable = false)
  private String action;

  @Column(name = "from_value", length = MAX_VALUE, updatable = false)
  private String fromValue;

  @Column(name = "to_value", length = MAX_VALUE, updatable = false)
  private String toValue;

  @Column(nullable = false, length = 50, updatable = false)
  private String actor;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected NoteHistory() {}

  /**
   * Records a change.
   *
   * @param clientId client
   * @param change what changed
   * @param actor user
   * @param occurredAt time
   */
  public NoteHistory(Long clientId, NoteChange change, String actor, Instant occurredAt) {
    this.clientId = clientId;
    this.itemKind = change.kind();
    this.itemRef = change.ref();
    this.action = change.action();
    this.fromValue = clip(change.from());
    this.toValue = clip(change.to());
    this.actor = actor;
    this.occurredAt = occurredAt;
  }

  private static String clip(String value) {
    return value == null || value.length() <= MAX_VALUE ? value : value.substring(0, MAX_VALUE);
  }

  public Long getId() {
    return id;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getItemKind() {
    return itemKind;
  }

  public String getItemRef() {
    return itemRef;
  }

  public String getAction() {
    return action;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public String getActor() {
    return actor;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
