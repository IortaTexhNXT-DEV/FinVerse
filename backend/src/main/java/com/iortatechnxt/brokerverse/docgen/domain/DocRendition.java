package com.iortatechnxt.brokerverse.docgen.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The content of a composed PDF, kept by the SHA-256 of the PDF so the same document can be
 * downloaded as Word (client requirement 16). Never changed once recorded.
 */
@Entity
@Table(name = "doc_rendition")
public class DocRendition extends BaseEntity {

  /** Longest title or reference kept. */
  public static final int TEXT_LENGTH = 200;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, length = TEXT_LENGTH, updatable = false)
  private String title;

  @Column(length = TEXT_LENGTH, updatable = false)
  private String reference;

  @Column(name = "doc_date", nullable = false, updatable = false)
  private LocalDate docDate;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String spec;

  protected DocRendition() {}

  /**
   * Records a rendition.
   *
   * @param sha256 SHA-256 of the PDF bytes (hex)
   * @param title document title
   * @param reference document reference, may be null
   * @param docDate date printed on the document
   * @param spec document content as JSON
   */
  public DocRendition(
      String sha256, String title, String reference, LocalDate docDate, String spec) {
    this.sha256 = sha256;
    this.title = shorten(title == null ? "Document" : title);
    this.reference = reference == null ? null : shorten(reference);
    this.docDate = docDate;
    this.spec = spec;
  }

  private static String shorten(String text) {
    return text.length() > TEXT_LENGTH ? text.substring(0, TEXT_LENGTH) : text;
  }

  public String getSha256() {
    return sha256;
  }

  public String getTitle() {
    return title;
  }

  public String getReference() {
    return reference;
  }

  public LocalDate getDocDate() {
    return docDate;
  }

  public String getSpec() {
    return spec;
  }
}
