package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * An entry of a code map version: the legacy code of a source system and its action (MAP to a
 * target code, DEFAULT, REJECT or CREATE). A PACKAGE entry may carry a qualifier (risk code,
 * insurer or sum-insured band) when one legacy package splits into several BIBS packages.
 */
@Entity
@Table(name = "mig_code_map_entry")
public class CodeMapEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "source_system", nullable = false, length = 10)
  private String sourceSystem;

  @Column(name = "legacy_code", nullable = false, length = 80)
  private String legacyCode;

  @Column(name = "legacy_description", length = 250)
  private String legacyDescription;

  @Column(nullable = false, length = 20)
  private String qualifier = "";

  @Column(name = "qualifier_value", nullable = false, length = 60)
  private String qualifierValue = "";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EntryAction action;

  @Column(name = "target_code", length = 60)
  private String targetCode;

  @Column(length = 300)
  private String remarks;

  protected CodeMapEntry() {}

  /**
   * A new entry of a version.
   *
   * @param versionId version
   * @param data entry data
   */
  public CodeMapEntry(Long versionId, EntryData data) {
    this.versionId = versionId;
    assign(data);
  }

  /**
   * Replaces the entry data.
   *
   * @param data entry data
   */
  public void apply(EntryData data) {
    assign(data);
  }

  private void assign(EntryData data) {
    this.sourceSystem = data.sourceSystem().strip().toUpperCase(Locale.ROOT);
    this.legacyCode = data.legacyCode().strip();
    this.legacyDescription = data.legacyDescription();
    this.qualifier =
        data.qualifier() == null ? "" : data.qualifier().strip().toUpperCase(Locale.ROOT);
    this.qualifierValue = data.qualifierValue() == null ? "" : data.qualifierValue().strip();
    this.action = data.action();
    this.targetCode =
        data.targetCode() == null || data.targetCode().isBlank() ? null : data.targetCode().strip();
    this.remarks = data.remarks();
  }

  /**
   * The same data for another version (copy of an approved version into a draft).
   *
   * @return entry data
   */
  public EntryData data() {
    return new EntryData(
        sourceSystem,
        legacyCode,
        legacyDescription,
        qualifier,
        qualifierValue,
        action,
        targetCode,
        remarks);
  }

  /**
   * Key of the entry within its version (source, legacy code and qualifier).
   *
   * @return key
   */
  public String key() {
    return sourceSystem + "|" + legacyCode + "|" + qualifier + "|" + qualifierValue;
  }

  public Long getId() {
    return id;
  }

  public Long getVersionId() {
    return versionId;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getLegacyCode() {
    return legacyCode;
  }

  public String getLegacyDescription() {
    return legacyDescription;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getQualifierValue() {
    return qualifierValue;
  }

  public EntryAction getAction() {
    return action;
  }

  public String getTargetCode() {
    return targetCode;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Data of an entry.
   *
   * @param sourceSystem source system
   * @param legacyCode legacy code
   * @param legacyDescription legacy description
   * @param qualifier RISK_CODE, INSURER or SI_BAND (PACKAGE entries), blank otherwise
   * @param qualifierValue value of the qualifier (code, or from-to amounts)
   * @param action action
   * @param targetCode target code (MAP)
   * @param remarks remarks
   */
  public record EntryData(
      String sourceSystem,
      String legacyCode,
      String legacyDescription,
      String qualifier,
      String qualifierValue,
      EntryAction action,
      String targetCode,
      String remarks) {

    /** Checks the mandatory parts of an entry: source, legacy code, action and a MAP target. */
    public void requireValid() {
      if (blank(sourceSystem) || blank(legacyCode) || action == null) {
        throw new BusinessRuleException(
            "MIG_MAP_ENTRY", "Enter the source system, the legacy code and the action");
      }
      if (action == EntryAction.MAP && blank(targetCode)) {
        throw new BusinessRuleException(
            "MIG_MAP_TARGET_REQUIRED", "Enter the target code of legacy code " + legacyCode);
      }
    }

    private static boolean blank(String s) {
      return s == null || s.isBlank();
    }
  }

  /**
   * The BIBS codes an approved version maps to, and the values it creates.
   *
   * @param entries entries of a version
   * @return targets
   */
  public static Targets targetsOf(List<CodeMapEntry> entries) {
    Set<String> mapped = new TreeSet<>();
    List<String> created = new ArrayList<>();
    for (CodeMapEntry e : entries) {
      if (e.getAction() == EntryAction.MAP || e.getAction() == EntryAction.DEFAULT) {
        Optional.ofNullable(e.getTargetCode()).ifPresent(mapped::add);
      } else if (e.getAction() == EntryAction.CREATE) {
        created.add(e.getTargetCode() == null ? e.getLegacyCode() : e.getTargetCode());
      }
    }
    return new Targets(mapped, created);
  }

  /**
   * Targets of a version.
   *
   * @param mapped existing BIBS codes mapped to
   * @param created codes created through the reference-data load
   */
  public record Targets(Set<String> mapped, List<String> created) {}
}
