package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * A package advisory (BRPM.016): drafted automatically when a request's version is released or its
 * package retired, edited by TSU, and sent to the recipient groups (list PKG_ADVISORY_GROUP) with
 * the supporting documents. Sending is blocked while the required documents are missing.
 */
@Entity
@Table(name = "pm_advisory")
public class Advisory extends BaseEntity {

  /** Advisory type. */
  public enum Type {
    /** A new or reactivated package is available. */
    PACKAGE_READY,
    /** An amended or updated package version is available. */
    PACKAGE_UPDATED,
    /** A renewed package version is available. */
    RENEWAL,
    /** A package was retired. */
    RETIREMENT
  }

  /** Advisory status. */
  public enum Status {
    /** Being prepared. */
    DRAFT,
    /** Sent. */
    SENT
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_id", updatable = false)
  private Long requestId;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "version_no", updatable = false)
  private Integer versionNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "advisory_type", nullable = false, length = 20, updatable = false)
  private Type advisoryType;

  @Column(name = "recipient_groups", nullable = false, length = 200)
  private String recipientGroups;

  @Column(name = "email_to", length = 1000)
  private String emailTo;

  @Column(nullable = false, length = 200)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "template_version", length = 60)
  private String templateVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Status status = Status.DRAFT;

  @Column(name = "document_ids", length = 300)
  private String documentIds;

  @Column(name = "message_ids", length = 300)
  private String messageIds;

  @Column(name = "sent_by", length = 50)
  private String sentBy;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected Advisory() {}

  /**
   * A draft advisory.
   *
   * @param companyId company
   * @param requestId request, null for a product-level advisory
   * @param productCode product
   * @param versionNo version concerned, null for a retirement
   * @param advisoryType type
   */
  public Advisory(
      Long companyId, Long requestId, String productCode, Integer versionNo, Type advisoryType) {
    this.companyId = companyId;
    this.requestId = requestId;
    this.productCode = productCode;
    this.versionNo = versionNo;
    this.advisoryType = advisoryType;
  }

  /**
   * Sets the content of the draft.
   *
   * @param content groups, addresses, subject, body and template version
   */
  public void compose(Content content) {
    this.recipientGroups = String.join(",", content.groups());
    this.emailTo = content.emailTo().isEmpty() ? null : String.join(",", content.emailTo());
    this.subject = content.subject();
    this.body = content.body();
    this.templateVersion = content.templateVersion();
  }

  /**
   * Records the send.
   *
   * @param documents supporting document ids
   * @param messages outbox message ids
   * @param user sender
   * @param when time
   */
  public void markSent(List<Long> documents, List<Long> messages, String user, Instant when) {
    this.documentIds = join(documents);
    this.messageIds = join(messages);
    this.sentBy = user;
    this.sentAt = when;
    this.status = Status.SENT;
  }

  private static String join(List<Long> ids) {
    return ids.isEmpty() ? null : String.join(",", ids.stream().map(String::valueOf).toList());
  }

  private static List<String> split(String csv) {
    return csv == null || csv.isBlank() ? List.of() : Arrays.asList(csv.split(","));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public String getProductCode() {
    return productCode;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public Type getAdvisoryType() {
    return advisoryType;
  }

  public List<String> getRecipientGroupList() {
    return split(recipientGroups);
  }

  public List<String> getEmailToList() {
    return split(emailTo);
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public Status getStatus() {
    return status;
  }

  public List<String> getDocumentIdList() {
    return split(documentIds);
  }

  public List<String> getMessageIdList() {
    return split(messageIds);
  }

  public String getSentBy() {
    return sentBy;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  /**
   * Content of an advisory.
   *
   * @param groups recipient groups
   * @param emailTo e-mail addresses (optional)
   * @param subject subject
   * @param body body
   * @param templateVersion template version tag
   */
  public record Content(
      List<String> groups,
      List<String> emailTo,
      String subject,
      String body,
      String templateVersion) {

    /** Defensive copies. */
    public Content {
      groups = groups == null ? List.of() : List.copyOf(groups);
      emailTo = emailTo == null ? List.of() : List.copyOf(emailTo);
    }
  }
}
