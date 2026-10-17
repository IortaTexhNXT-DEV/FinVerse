package com.iortatechnxt.brokerverse.renewal.submitted;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The terms a submitted policy was handed to Renewal with (V1017; RENEWAL_DESIGN section 2.3): the
 * RA template (generic or Free First Year), the insurer assigned by the insurer rules of Submitted
 * Policies, the handler and Account Officer, the mailing address of printed letters and whether the
 * renewal was started by hand (Renew with BDOI).
 */
@Entity
@Table(name = "rnw_submitted_handoff")
public class SubmittedHandOffRecord extends BaseEntity {

  /** RA template of a Free First Year policy. */
  public static final String FFY = "FFY";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "sbm_no", nullable = false, updatable = false, length = 30)
  private String sbmNo;

  @Column(name = "ra_template", nullable = false, length = 20)
  private String raTemplate;

  @Column(name = "assigned_insurer", length = 30)
  private String assignedInsurer;

  @Column(name = "handler_username", length = 50)
  private String handlerUsername;

  @Column(name = "ao_username", length = 50)
  private String aoUsername;

  @Column(name = "mailing_address", length = 500)
  private String mailingAddress;

  @Column(name = "manual", nullable = false)
  private boolean manual;

  /** For JPA. */
  protected SubmittedHandOffRecord() {}

  /**
   * Records a hand-off.
   *
   * @param companyId company
   * @param candidateId renewal candidate
   * @param sbmNo masterlist number
   * @param terms template, insurer, handler, AO, address and manual flag
   */
  public SubmittedHandOffRecord(Long companyId, Long candidateId, String sbmNo, Terms terms) {
    this.companyId = companyId;
    this.candidateId = candidateId;
    this.sbmNo = sbmNo;
    this.raTemplate = FFY.equals(terms.raTemplate()) ? FFY : "GENERIC";
    this.assignedInsurer = terms.assignedInsurer();
    this.handlerUsername = terms.handlerUsername();
    this.aoUsername = terms.aoUsername();
    this.mailingAddress = terms.mailingAddress();
    this.manual = terms.manual();
  }

  /**
   * Whether the RA is the Free First Year variant.
   *
   * @return true for FFY
   */
  public boolean isFreeFirstYear() {
    return FFY.equals(raTemplate);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getSbmNo() {
    return sbmNo;
  }

  public String getRaTemplate() {
    return raTemplate;
  }

  public String getAssignedInsurer() {
    return assignedInsurer;
  }

  public String getHandlerUsername() {
    return handlerUsername;
  }

  public String getAoUsername() {
    return aoUsername;
  }

  public String getMailingAddress() {
    return mailingAddress;
  }

  public boolean isManual() {
    return manual;
  }

  /**
   * The terms of a hand-off.
   *
   * @param raTemplate GENERIC or FFY
   * @param assignedInsurer insurer assigned
   * @param handlerUsername handler
   * @param aoUsername Account Officer
   * @param mailingAddress address of printed letters
   * @param manual Renew with BDOI
   */
  public record Terms(
      String raTemplate,
      String assignedInsurer,
      String handlerUsername,
      String aoUsername,
      String mailingAddress,
      boolean manual) {}
}
