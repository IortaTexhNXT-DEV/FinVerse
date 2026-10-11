package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Setting of one renewal check (BRRN.020; RENEWAL_DESIGN section 4.2): active or not, the severity
 * of a failure and optional parameters. Maintained with maker-checker; the authorized version stays
 * in force until the change is authorized.
 */
@Entity
@Table(name = "rnw_check_setting")
public class CheckSetting extends AuthorizableEntity {

  @Column(name = "check_code", nullable = false, length = 40, updatable = false)
  private String checkCode;

  @Column(nullable = false)
  private boolean active;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CheckSeverity severity;

  @Column(length = 1000)
  private String parameters;

  protected CheckSetting() {}

  /**
   * Creates a setting pending authorization.
   *
   * @param checkCode check
   * @param active active
   * @param severity severity
   * @param parameters parameters, may be null
   */
  public CheckSetting(String checkCode, boolean active, CheckSeverity severity, String parameters) {
    this.checkCode = checkCode;
    this.active = active;
    this.severity = severity;
    this.parameters = parameters;
  }

  /**
   * Changes the setting; it must be authorized again.
   *
   * @param newActive active
   * @param newSeverity severity
   * @param newParameters parameters
   */
  public void update(boolean newActive, CheckSeverity newSeverity, String newParameters) {
    this.active = newActive;
    this.severity = newSeverity;
    this.parameters = newParameters;
    markModified();
  }

  public String getCheckCode() {
    return checkCode;
  }

  public boolean isEnabled() {
    return active;
  }

  public CheckSeverity getSeverity() {
    return severity;
  }

  public String getParameters() {
    return parameters;
  }
}
