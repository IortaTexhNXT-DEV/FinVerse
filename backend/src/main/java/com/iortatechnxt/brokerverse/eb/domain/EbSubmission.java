package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Documents submitted to an insurer for a process (BRID-026; FR-EB-034): new-business or renewal
 * placement, adjustment, endorsement, franchise or proposal, on a cycle or a member change, sent by
 * protected e-mail after the required-document check.
 */
@Entity
@Table(name = "eb_submission")
public class EbSubmission extends EbCycleRecord {

  @Column(name = "member_change_id", updatable = false)
  private Long memberChangeId;

  @Column(name = "process_type", nullable = false, length = 30, updatable = false)
  private String processType;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "sent_at", nullable = false, updatable = false)
  private Instant sentAt;

  @Column(name = "sent_by", nullable = false, length = 50, updatable = false)
  private String sentBy;

  @Column(name = "message_id")
  private Long messageId;

  @Column(nullable = false, length = 1000)
  private String recipients;

  @Column(name = "acknowledged_on")
  private LocalDate acknowledgedOn;

  @Column(length = 1000)
  private String remarks;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<Document> documents = new ArrayList<>();

  protected EbSubmission() {}

  /**
   * Records a submission.
   *
   * @param scope company, programme and cycle (the cycle may be null)
   * @param memberChangeId member change, may be null
   * @param processType process (list EB_PROCESS_TYPE)
   * @param insurerCode insurer
   * @param sending time, user, recipients and remarks
   */
  public EbSubmission(
      Scope scope, Long memberChangeId, String processType, String insurerCode, Sending sending) {
    super(scope.companyId(), scope.programmeId(), scope.cycleId());
    this.memberChangeId = memberChangeId;
    this.processType = processType;
    this.insurerCode = insurerCode;
    this.sentAt = sending.at();
    this.sentBy = sending.by();
    this.recipients = sending.recipients();
    this.remarks = sending.remarks();
  }

  /**
   * Adds a document sent.
   *
   * @param attachmentId stored file
   * @param documentType its type
   */
  public void addDocument(Long attachmentId, String documentType) {
    documents.add(new Document(this, attachmentId, documentType));
  }

  /**
   * Keeps the outbox message.
   *
   * @param message message id
   */
  public void sentAs(Long message) {
    this.messageId = message;
  }

  /**
   * Records the insurer's acknowledgement.
   *
   * @param date date acknowledged
   */
  public void acknowledge(LocalDate date) {
    this.acknowledgedOn = date;
  }

  public Long getMemberChangeId() {
    return memberChangeId;
  }

  public String getProcessType() {
    return processType;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentBy() {
    return sentBy;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getRecipients() {
    return recipients;
  }

  public LocalDate getAcknowledgedOn() {
    return acknowledgedOn;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<Document> getDocuments() {
    return Collections.unmodifiableList(documents);
  }

  /**
   * Where a submission belongs.
   *
   * @param companyId company
   * @param programmeId programme
   * @param cycleId cycle, may be null
   */
  public record Scope(Long companyId, Long programmeId, Long cycleId) {}

  /**
   * When, by whom and to whom it is sent.
   *
   * @param at time
   * @param by user
   * @param recipients e-mail addresses
   * @param remarks remarks, may be null
   */
  public record Sending(Instant at, String by, String recipients, String remarks) {}

  /** A document of a submission. */
  @Entity(name = "EbSubmissionDocument")
  @Table(name = "eb_submission_document")
  public static class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, updatable = false)
    private EbSubmission submission;

    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    protected Document() {}

    Document(EbSubmission submission, Long attachmentId, String documentType) {
      this.submission = submission;
      this.attachmentId = attachmentId;
      this.documentType = documentType;
    }

    public Long getAttachmentId() {
      return attachmentId;
    }

    public String getDocumentType() {
      return documentType;
    }
  }
}
