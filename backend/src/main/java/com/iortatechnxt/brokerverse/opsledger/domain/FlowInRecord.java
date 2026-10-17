package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One record received through a feed, keyed by its idempotency key so it is processed once
 * (BRQID.005 "no duplicates"), with the hash of its payload. A failed record may be sent again: the
 * next attempt replaces its outcome.
 */
@Entity
@Table(name = "ops_flow_in_record")
public class FlowInRecord {

  private static final int MAX_MESSAGE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "feed_code", nullable = false, length = 40, updatable = false)
  private String feedCode;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
  private String idempotencyKey;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FlowInEnums.RecordStatus status;

  @Column(length = 100)
  private String reference;

  @Column(length = MAX_MESSAGE)
  private String message;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected FlowInRecord() {}

  /**
   * A record received.
   *
   * @param feedCode feed
   * @param runId run
   * @param idempotencyKey key
   * @param payloadHash SHA-256 of the payload
   * @param at time
   */
  public FlowInRecord(
      String feedCode, Long runId, String idempotencyKey, String payloadHash, Instant at) {
    this.feedCode = feedCode;
    this.runId = runId;
    this.idempotencyKey = idempotencyKey;
    this.payloadHash = payloadHash;
    this.receivedAt = at;
    this.status = FlowInEnums.RecordStatus.FAILED;
  }

  /**
   * Records the outcome of an attempt.
   *
   * @param attemptRun run of the attempt
   * @param hash payload hash of the attempt
   * @param outcome accepted or failed
   * @param ref record created, may be null
   * @param text message, may be null
   * @param at time
   */
  public void attempted(
      Long attemptRun,
      String hash,
      FlowInEnums.RecordStatus outcome,
      String ref,
      String text,
      Instant at) {
    this.runId = attemptRun;
    this.payloadHash = hash;
    this.status = outcome;
    this.reference = ref;
    this.message =
        text == null || text.length() <= MAX_MESSAGE ? text : text.substring(0, MAX_MESSAGE);
    this.receivedAt = at;
  }

  public Long getId() {
    return id;
  }

  public String getFeedCode() {
    return feedCode;
  }

  public Long getRunId() {
    return runId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getPayloadHash() {
    return payloadHash;
  }

  public FlowInEnums.RecordStatus getStatus() {
    return status;
  }

  public String getReference() {
    return reference;
  }

  public String getMessage() {
    return message;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}
