package com.iortatechnxt.brokerverse.migration.signoff.domain;

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
 * A sign-off of a gate (DATA_MIGRATION_DESIGN section 13; FR-DM-003): object, batch, gate, the role
 * and user who signed, the decision, comment, time and optional evidence file. Sign-offs are never
 * changed; a correction is a new sign-off.
 */
@Entity
@Table(name = "mig_signoff")
public class MigSignoff {

  /** Decision of a sign-off. */
  public enum Decision {
    APPROVED,
    REJECTED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "batch_id", updatable = false)
  private Long batchId;

  @Column(name = "cutover_plan_id", updatable = false)
  private Long cutoverPlanId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 4, updatable = false)
  private Gate gate;

  @Column(name = "role_code", nullable = false, length = 40, updatable = false)
  private String roleCode;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Decision decision;

  @Column(length = 2000, updatable = false)
  private String comment;

  @Column(name = "evidence_file_id")
  private Long evidenceFileId;

  @Column(name = "evidence_name", length = 255)
  private String evidenceName;

  @Column(name = "signed_at", nullable = false, updatable = false)
  private Instant signedAt;

  protected MigSignoff() {}

  /**
   * A sign-off.
   *
   * @param companyId company
   * @param scope object, batch and cutover plan
   * @param gate gate
   * @param signer role and user
   * @param decision decision
   * @param comment comment
   * @param signedAt time
   */
  public MigSignoff(
      Long companyId,
      Scope scope,
      Gate gate,
      Signer signer,
      Decision decision,
      String comment,
      Instant signedAt) {
    this.companyId = companyId;
    this.objectCode = scope.objectCode();
    this.batchId = scope.batchId();
    this.cutoverPlanId = scope.cutoverPlanId();
    this.gate = gate;
    this.roleCode = signer.role();
    this.username = signer.username();
    this.decision = decision;
    this.comment = comment;
    this.signedAt = signedAt;
  }

  /**
   * Keeps the evidence file.
   *
   * @param fileId stored file
   * @param name file name
   */
  public void evidence(Long fileId, String name) {
    this.evidenceFileId = fileId;
    this.evidenceName = name;
  }

  public boolean approved() {
    return decision == Decision.APPROVED;
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getCutoverPlanId() {
    return cutoverPlanId;
  }

  public Gate getGate() {
    return gate;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public String getUsername() {
    return username;
  }

  public Decision getDecision() {
    return decision;
  }

  public String getComment() {
    return comment;
  }

  public Long getEvidenceFileId() {
    return evidenceFileId;
  }

  public String getEvidenceName() {
    return evidenceName;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  /**
   * What is signed.
   *
   * @param objectCode object
   * @param batchId batch (null for G1, G2 and G7)
   * @param cutoverPlanId cutover plan (G7)
   */
  public record Scope(String objectCode, Long batchId, Long cutoverPlanId) {}

  /**
   * Who signs.
   *
   * @param role role code the user signs in
   * @param username user
   */
  public record Signer(String role, String username) {}
}
