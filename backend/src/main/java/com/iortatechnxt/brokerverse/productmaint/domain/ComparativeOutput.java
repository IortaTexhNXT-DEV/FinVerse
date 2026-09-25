package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * A stored comparative output of a package request (BRPM.014, PMADD03): the audit MASTER compiled
 * from a negotiation round (one current master per request; earlier masters are kept as
 * superseded), or a CLIENT view derived from a master with selected fields and insurers. The
 * compiled table is kept as JSON ({@code content}) so every file is rendered from the values the
 * output was generated with; the PDF is attached to the request with its SHA-256.
 */
@Entity
@Table(name = "pm_comparative_output")
public class ComparativeOutput extends BaseEntity {

  /** Kind of an output. */
  public enum Kind {
    /** Audit master with every field and insurer. */
    MASTER,
    /** Client-tailored view of a master. */
    CLIENT
  }

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Kind kind;

  @Column(name = "parent_output_id", updatable = false)
  private Long parentOutputId;

  @Column(name = "is_current", nullable = false)
  private boolean current = true;

  @Column(nullable = false, length = 200, updatable = false)
  private String title;

  @Column(nullable = false, length = 500, updatable = false)
  private String fields;

  @Column(nullable = false, length = 500, updatable = false)
  private String insurers;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String content;

  @Column(name = "template_version", nullable = false, length = 60, updatable = false)
  private String templateVersion;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(nullable = false, length = 64)
  private String sha256;

  protected ComparativeOutput() {}

  /**
   * A new output (not yet saved).
   *
   * @param requestId request
   * @param roundNo round compiled
   * @param kind master or client
   * @param parentOutputId master of a client output, null for a master
   * @param spec title, selections, content and template version
   */
  public ComparativeOutput(
      Long requestId, int roundNo, Kind kind, Long parentOutputId, OutputSpec spec) {
    this.requestId = requestId;
    this.roundNo = roundNo;
    this.kind = kind;
    this.parentOutputId = parentOutputId;
    this.title = spec.title();
    this.fields = String.join(",", spec.fields());
    this.insurers = String.join(",", spec.insurers());
    this.content = spec.content();
    this.templateVersion = spec.templateVersion();
    this.sha256 = "";
  }

  /**
   * Records the stored file.
   *
   * @param attachment attachment id
   * @param hash SHA-256 of the file
   */
  public void store(Long attachment, String hash) {
    this.attachmentId = attachment;
    this.sha256 = hash;
  }

  /** Marks a master superseded by a newer one (only one current master, BRPM.014). */
  public void supersede() {
    this.current = false;
  }

  public Long getRequestId() {
    return requestId;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public Kind getKind() {
    return kind;
  }

  public Long getParentOutputId() {
    return parentOutputId;
  }

  public boolean isCurrent() {
    return current;
  }

  public String getTitle() {
    return title;
  }

  public List<String> getFieldList() {
    return split(fields);
  }

  public List<String> getInsurerList() {
    return split(insurers);
  }

  public String getContent() {
    return content;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getSha256() {
    return sha256;
  }

  private static List<String> split(String csv) {
    return csv == null || csv.isBlank() ? List.of() : Arrays.asList(csv.split(","));
  }

  /**
   * What an output is generated from.
   *
   * @param title title
   * @param fields selected fields (field catalogue codes)
   * @param insurers selected insurer codes
   * @param content compiled table as JSON
   * @param templateVersion template version tag
   */
  public record OutputSpec(
      String title,
      List<String> fields,
      List<String> insurers,
      String content,
      String templateVersion) {

    /** Defensive copies. */
    public OutputSpec {
      fields = List.copyOf(fields);
      insurers = List.copyOf(insurers);
    }
  }
}
