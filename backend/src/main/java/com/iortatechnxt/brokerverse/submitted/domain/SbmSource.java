package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * An approved source of submitted policies (BRIDSP-01; FRS BRD-12 FR-SP-001): its segment and
 * business type when fixed, the bulk handler that loads its files and, for manual entry, the
 * mandatory fields.
 */
@Entity
@Table(name = "sbm_source")
public class SbmSource extends BaseEntity {

  @Column(nullable = false, updatable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 30)
  private String segment;

  @Column(name = "business_type", length = 2)
  private String businessType;

  @Column(name = "bulk_handler", length = 40)
  private String bulkHandler;

  @Column(nullable = false, length = 20)
  private String format;

  @Column(name = "mandatory_fields", length = 500)
  private String mandatoryFields;

  @Column(nullable = false)
  private boolean active;

  protected SbmSource() {}

  /**
   * Changes the maintainable fields (Submitted Policies Setup).
   *
   * @param newName name
   * @param newMandatoryFields mandatory fields, comma separated
   * @param isActive active
   */
  public final void maintain(String newName, String newMandatoryFields, boolean isActive) {
    this.name = newName;
    this.mandatoryFields = newMandatoryFields;
    this.active = isActive;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getSegment() {
    return segment;
  }

  public String getBusinessType() {
    return businessType;
  }

  public String getBulkHandler() {
    return bulkHandler;
  }

  public String getFormat() {
    return format;
  }

  public String getMandatoryFields() {
    return mandatoryFields;
  }

  /**
   * The mandatory fields as a list.
   *
   * @return field names
   */
  public List<String> mandatory() {
    return mandatoryFields == null || mandatoryFields.isBlank()
        ? List.of()
        : Arrays.stream(mandatoryFields.split(",")).map(String::strip).toList();
  }

  public boolean isActive() {
    return active;
  }
}
