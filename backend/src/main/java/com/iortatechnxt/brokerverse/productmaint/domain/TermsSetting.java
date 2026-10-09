package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * The choices of a comparative table (BDOI FRS FRPM.006.02, FRPM.008.01): the fields shown in the
 * table, the fields sent to the client, and the insurers the requestor selected for the proposal.
 */
@Entity
@Table(name = "pm_terms_setting")
public class TermsSetting extends BaseEntity {

  private static final String SEPARATOR = ",";

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(name = "display_fields", length = 500)
  private String displayFields;

  @Column(name = "client_fields", length = 500)
  private String clientFields;

  @Column(name = "selected_insurers", length = 500)
  private String selectedInsurers;

  @Column(name = "selected_by", length = 50)
  private String selectedBy;

  @Column(name = "selected_at")
  private Instant selectedAt;

  /** For JPA. */
  protected TermsSetting() {}

  /**
   * Creates the choices of a record.
   *
   * @param record the record
   */
  public TermsSetting(TermsRecord record) {
    this.recordType = record.type();
    this.recordId = record.id();
  }

  /**
   * Sets the fields shown and sent to the client.
   *
   * @param shown fields shown in the table
   * @param client fields sent to the client
   */
  public void fields(List<String> shown, List<String> client) {
    this.displayFields = join(shown);
    this.clientFields = join(client);
  }

  /**
   * Records the insurers selected for the proposal.
   *
   * @param insurers insurer codes
   * @param user requestor
   * @param when time
   */
  public void select(List<String> insurers, String user, Instant when) {
    this.selectedInsurers = join(insurers);
    this.selectedBy = user;
    this.selectedAt = when;
  }

  private static String join(List<String> values) {
    return values == null || values.isEmpty() ? null : String.join(SEPARATOR, values);
  }

  private static List<String> split(String value) {
    return value == null || value.isBlank()
        ? List.of()
        : Arrays.stream(value.split(SEPARATOR))
            .map(String::strip)
            .filter(v -> !v.isEmpty())
            .toList();
  }

  public List<String> getDisplayFields() {
    return split(displayFields);
  }

  public List<String> getClientFields() {
    return split(clientFields);
  }

  public List<String> getSelectedInsurers() {
    return split(selectedInsurers);
  }

  public String getSelectedBy() {
    return selectedBy;
  }

  public Instant getSelectedAt() {
    return selectedAt;
  }
}
