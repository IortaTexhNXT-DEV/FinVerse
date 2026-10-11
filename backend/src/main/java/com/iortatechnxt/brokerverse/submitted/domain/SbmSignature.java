package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** The signature of an approval level of an IAAF or TOR (BRIDSP-07, 18). */
@Entity
@Table(name = "sbm_signature")
public class SbmSignature extends BaseEntity {

  @Column(name = "document_type", nullable = false, updatable = false, length = 10)
  private String documentType;

  @Column(name = "document_id", nullable = false, updatable = false)
  private Long documentId;

  @Column(nullable = false, updatable = false)
  private int level;

  @Column(nullable = false, updatable = false, length = 50)
  private String signer;

  @Column(name = "signer_name", nullable = false, updatable = false, length = 150)
  private String signerName;

  @Column(nullable = false, updatable = false, length = 120)
  private String position;

  @Column(name = "signed_at", nullable = false, updatable = false)
  private Instant signedAt;

  @Column(nullable = false, updatable = false, length = 10)
  private String method;

  @Column(nullable = false, updatable = false, length = 64)
  private String hash;

  protected SbmSignature() {}

  /**
   * A signature.
   *
   * @param document document type and id
   * @param level level
   * @param signer signer, name and position
   * @param stamp time, method and hash
   */
  public SbmSignature(Document document, int level, Signer signer, Stamp stamp) {
    this.documentType = document.type();
    this.documentId = document.id();
    this.level = level;
    this.signer = signer.username();
    this.signerName = signer.name();
    this.position = signer.position();
    this.signedAt = stamp.at();
    this.method = stamp.method();
    this.hash = stamp.hash();
  }

  public String getDocumentType() {
    return documentType;
  }

  public Long getDocumentId() {
    return documentId;
  }

  public int getLevel() {
    return level;
  }

  public String getSigner() {
    return signer;
  }

  public String getSignerName() {
    return signerName;
  }

  public String getPosition() {
    return position;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public String getMethod() {
    return method;
  }

  public String getHash() {
    return hash;
  }

  /**
   * The signed document.
   *
   * @param type IAAF or TOR
   * @param id document id
   */
  public record Document(String type, Long id) {}

  /**
   * Who signed.
   *
   * @param username login
   * @param name display name
   * @param position signatory title
   */
  public record Signer(String username, String name, String position) {}

  /**
   * The stamp.
   *
   * @param at time
   * @param method STAMPED or ESIG
   * @param hash hash
   */
  public record Stamp(Instant at, String method, String hash) {}
}
