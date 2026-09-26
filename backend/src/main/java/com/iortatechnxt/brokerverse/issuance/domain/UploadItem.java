package com.iortatechnxt.brokerverse.issuance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Objects;

/** One file of a bulk e-policy upload with its proposed (or chosen) account and outcome. */
@Entity
@Table(name = "iss_upload_item")
public class UploadItem {

  private static final int MAX_TEXT = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false, updatable = false)
  private UploadBatch batch;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  @Column(length = 30)
  private String arn;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_method", length = 20)
  private MatchMethod matchMethod;

  @Column(length = MAX_TEXT)
  private String message;

  @Column(nullable = false)
  private boolean included = true;

  @Column(name = "epolicy_id")
  private Long epolicyId;

  @Column(length = MAX_TEXT)
  private String outcome;

  protected UploadItem() {}

  UploadItem(UploadBatch batch, int lineNo, HeldFile file) {
    this.batch = batch;
    this.lineNo = lineNo;
    this.fileName = file.fileName();
    this.sha256 = file.sha256();
    this.content = file.content();
  }

  /**
   * Records the proposed match.
   *
   * @param matchedArn ARN found, null when none
   * @param method how it was found
   * @param note explanation
   */
  public void propose(String matchedArn, MatchMethod method, String note) {
    this.arn = matchedArn;
    this.matchMethod = method;
    this.message = clip(note);
    this.included = matchedArn != null;
  }

  /**
   * Records the user's choice.
   *
   * @param chosenArn account chosen, null to keep the proposal
   * @param include whether the file is stored on confirmation
   */
  public void choose(String chosenArn, boolean include) {
    if (chosenArn != null && !chosenArn.isBlank() && !chosenArn.strip().equals(arn)) {
      this.arn = chosenArn.strip();
      this.matchMethod = MatchMethod.MANUAL;
      this.message = "Chosen by the user";
    }
    this.included = include && arn != null;
  }

  /**
   * Records the outcome of the confirmation.
   *
   * @param storedEpolicyId e-policy created, null when refused
   * @param note outcome or reason
   */
  public void done(Long storedEpolicyId, String note) {
    this.epolicyId = storedEpolicyId;
    this.outcome = clip(note);
  }

  private static String clip(String text) {
    return text == null || text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
  }

  public Long getId() {
    return id;
  }

  public UploadBatch getBatch() {
    return batch;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public byte[] getContent() {
    return content.clone();
  }

  public String getArn() {
    return arn;
  }

  public MatchMethod getMatchMethod() {
    return matchMethod;
  }

  public String getMessage() {
    return message;
  }

  public boolean isIncluded() {
    return included;
  }

  public Long getEpolicyId() {
    return epolicyId;
  }

  public String getOutcome() {
    return outcome;
  }

  /**
   * A file held until the upload is confirmed.
   *
   * @param fileName file name
   * @param sha256 checksum
   * @param content bytes
   */
  public record HeldFile(String fileName, String sha256, byte[] content) {

    /** Defensive copy. */
    public HeldFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof HeldFile f
          && Objects.equals(fileName, f.fileName)
          && Objects.equals(sha256, f.sha256)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileName, sha256, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return fileName + " (" + content.length + " bytes)";
    }
  }
}
