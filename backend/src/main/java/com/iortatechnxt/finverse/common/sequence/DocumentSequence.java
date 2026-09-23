package com.iortatechnxt.finverse.common.sequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Gapless counter per sequence key (e.g. "JV-HO-2026"). Updated under a row lock. */
@Entity
@Table(name = "document_sequence")
public class DocumentSequence {

  @Id
  @Column(name = "sequence_key", length = 60)
  private String sequenceKey;

  @Column(name = "next_value", nullable = false)
  private long nextValue;

  protected DocumentSequence() {}

  /**
   * Creates a sequence starting at 1.
   *
   * @param sequenceKey key
   */
  public DocumentSequence(String sequenceKey) {
    this.sequenceKey = sequenceKey;
    this.nextValue = 1;
  }

  /**
   * Returns the current value and advances the counter.
   *
   * @return allocated value
   */
  public long allocate() {
    return nextValue++;
  }

  public String getSequenceKey() {
    return sequenceKey;
  }
}
