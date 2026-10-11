package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Set;

/**
 * The routing facts of a package request in BDOI's FRS: the Source (Marketing, TSU or Insurer,
 * FRPM.011.02), the Annex E details kept as JSON, the Marketing approvals given (Team Leader, Team
 * Head, Unit Head) and the Package Deployment Request (number, date and time, requestor,
 * FRPM.015.01).
 */
@Embeddable
public class RequestRouting {

  /** Source of a request raised by Marketing. */
  public static final String MARKETING = "MARKETING";

  /** Source of a request raised by TSU. */
  public static final String TSU = "TSU";

  /** Source of a request on an insurer's offer. */
  public static final String INSURER = "INSURER";

  private static final Set<String> SOURCES = Set.of(MARKETING, TSU, INSURER);

  @Column(nullable = false, length = 20)
  private String source = MARKETING;

  @Column(name = "request_details", columnDefinition = "text")
  private String details;

  @Column(name = "marketing_level", nullable = false)
  private int marketingLevel;

  @Column(name = "deployment_no", length = 30)
  private String deploymentNo;

  @Column(name = "deployment_requested_at")
  private Instant deploymentRequestedAt;

  @Column(name = "deployment_requested_by", length = 50)
  private String deploymentRequestedBy;

  /**
   * Sets the source and the Annex E details of the form.
   *
   * @param source MARKETING, TSU or INSURER; null keeps Marketing
   * @param details Annex E details as JSON, may be null
   */
  public void describe(String source, String details) {
    String value = source == null || source.isBlank() ? MARKETING : source.strip();
    if (!SOURCES.contains(value)) {
      throw new BusinessRuleException(
          "PKG_SOURCE", "The source of a package request is Marketing, TSU or Insurer");
    }
    this.source = value;
    this.details = details;
  }

  /**
   * Records one more Marketing approval.
   *
   * @return the number of Marketing approvals given
   */
  public int approveLevel() {
    marketingLevel++;
    return marketingLevel;
  }

  /** Starts the Marketing approvals again (a returned request is submitted anew). */
  public void resetLevels() {
    marketingLevel = 0;
  }

  /**
   * Records the Package Deployment Request.
   *
   * @param number deployment request number
   * @param user requestor
   * @param when date and time
   */
  public void requestDeployment(String number, String user, Instant when) {
    this.deploymentNo = number;
    this.deploymentRequestedBy = user;
    this.deploymentRequestedAt = when;
  }

  /**
   * Whether the request comes from Marketing (Marketing approval before TSU).
   *
   * @return true for Marketing
   */
  public boolean fromMarketing() {
    return MARKETING.equals(source);
  }

  public String getSource() {
    return source;
  }

  public String getDetails() {
    return details;
  }

  public int getMarketingLevel() {
    return marketingLevel;
  }

  public String getDeploymentNo() {
    return deploymentNo;
  }

  public Instant getDeploymentRequestedAt() {
    return deploymentRequestedAt;
  }

  public String getDeploymentRequestedBy() {
    return deploymentRequestedBy;
  }
}
