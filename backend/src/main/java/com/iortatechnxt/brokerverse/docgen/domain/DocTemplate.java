package com.iortatechnxt.brokerverse.docgen.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One version of a document template. Versions are never changed: a new text is a new version
 * effective from a date, and generated records store the version they used (BRNB.004).
 */
@Entity
@Table(name = "doc_template")
public class DocTemplate extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(nullable = false, length = 200, updatable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String body;

  @Column(name = "effective_from", nullable = false, updatable = false)
  private LocalDate effectiveFrom;

  @Column(nullable = false)
  private boolean active = true;

  protected DocTemplate() {}

  /**
   * Creates a version.
   *
   * @param code template code
   * @param versionNo version number
   * @param title title
   * @param body text with placeholders
   * @param effectiveFrom first date of use
   */
  public DocTemplate(
      String code, int versionNo, String title, String body, LocalDate effectiveFrom) {
    this.code = code;
    this.versionNo = versionNo;
    this.title = title;
    this.body = body;
    this.effectiveFrom = effectiveFrom;
  }

  /** Withdraws the version (it stays on documents already generated). */
  public void deactivate() {
    this.active = false;
  }

  public String getCode() {
    return code;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public boolean isActive() {
    return active;
  }
}
