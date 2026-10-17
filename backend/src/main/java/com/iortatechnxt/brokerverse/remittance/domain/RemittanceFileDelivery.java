package com.iortatechnxt.brokerverse.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One copy of an extract file of a batch to the shared folder (ICD IF-RMT-04). */
@Entity
@Table(name = "rem_file_delivery")
public class RemittanceFileDelivery {

  private static final int MAX_TEXT = 250;
  private static final int MAX_REFERENCE = 120;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Stage stage;

  @Column(name = "file_name", nullable = false, length = 120, updatable = false)
  private String fileName;

  @Column(nullable = false, length = MAX_TEXT, updatable = false)
  private String folder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Status status;

  @Column(length = 120, updatable = false)
  private String reference;

  @Column(length = MAX_TEXT, updatable = false)
  private String error;

  @Column(name = "attempted_at", nullable = false, updatable = false)
  private Instant attemptedAt;

  @Column(name = "attempted_by", length = 50, updatable = false)
  private String attemptedBy;

  /** For JPA. */
  protected RemittanceFileDelivery() {}

  /**
   * Records a copy.
   *
   * @param batchId batch
   * @param stage when the copy was made
   * @param target folder and file name
   * @param outcome delivered with the location, or failed with the reason
   * @param by user, null for the system
   * @param at time
   */
  public RemittanceFileDelivery(
      Long batchId, Stage stage, Target target, Outcome outcome, String by, Instant at) {
    this.batchId = batchId;
    this.stage = stage;
    this.folder = cut(target.folder());
    this.fileName = target.fileName();
    this.status = outcome.delivered() ? Status.DELIVERED : Status.FAILED;
    this.reference = outcome.delivered() ? cut(outcome.text(), MAX_REFERENCE) : null;
    this.error = outcome.delivered() ? null : cut(outcome.text());
    this.attemptedBy = by;
    this.attemptedAt = at;
  }

  private static String cut(String text) {
    return cut(text, MAX_TEXT);
  }

  private static String cut(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getId() {
    return id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Stage getStage() {
    return stage;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFolder() {
    return folder;
  }

  public Status getStatus() {
    return status;
  }

  public String getReference() {
    return reference;
  }

  public String getError() {
    return error;
  }

  public Instant getAttemptedAt() {
    return attemptedAt;
  }

  public String getAttemptedBy() {
    return attemptedBy;
  }

  /** When the copy was made. */
  public enum Stage {
    /** At the creation of the batch. */
    CREATED,
    /** At its approval. */
    APPROVED,
    /** Sent again by a user. */
    RESENT
  }

  /** Result of the copy. */
  public enum Status {
    /** Written to the folder. */
    DELIVERED,
    /** Not written. */
    FAILED
  }

  /**
   * Where the file goes.
   *
   * @param folder folder
   * @param fileName file name
   */
  public record Target(String folder, String fileName) {}

  /**
   * What the shared drive answered.
   *
   * @param delivered whether written
   * @param text location, or the reason
   */
  public record Outcome(boolean delivered, String text) {}
}
